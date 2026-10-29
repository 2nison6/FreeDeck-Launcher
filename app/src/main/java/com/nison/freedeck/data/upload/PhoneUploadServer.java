package com.nison.freedeck.data.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * A tiny web server that runs only while the "add a picture from your phone" popup is open. A phone on the same
 * Wi-Fi opens the address shown on the TV, picks a photo, and the photo is sent here.
 * <p>
 * Only requests under a random secret path are answered, so other devices on the network cannot find it by
 * guessing, and it takes a single picture of at most 10 MB before it stops being useful.
 */
public class PhoneUploadServer {

    /** Told, on the server's own thread, when a picture has arrived (as a file) or when the server stopped. */
    public interface Listener {
        void onPictureReceived(File picture);
    }

    private static final int PREFERRED_PORT = 8765;
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    /** The address stops working this long after it was handed out. */
    private static final long LIFETIME_MS = 5 * 60 * 1000L;
    private static final int SOCKET_TIMEOUT_MS = 30_000;
    private static final String TOKEN_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final File cacheDirectory;
    private final Listener listener;
    private final String token = newToken();

    private ServerSocket server;
    private volatile boolean running;
    private volatile boolean used;
    private volatile long expiresAt;

    public PhoneUploadServer(File cacheDirectory, Listener listener) {
        this.cacheDirectory = cacheDirectory;
        this.listener = listener;
    }

    /** Starts listening. @return the path part of the address (after the host and port), such as "/k3x9q2" */
    public String start() throws IOException {
        try {
            server = new ServerSocket(PREFERRED_PORT, 4, (InetAddress) null);
        } catch (IOException busy) {
            server = new ServerSocket(0, 4, (InetAddress) null);
        }
        expiresAt = System.currentTimeMillis() + LIFETIME_MS;
        running = true;
        Thread thread = new Thread(this::serve, "phone-upload");
        thread.setDaemon(true);
        thread.start();
        return "/" + token;
    }

    public int port() {
        return server.getLocalPort();
    }

    public void stop() {
        running = false;
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException ignored) {
            // closing is best effort
        }
    }

    private void serve() {
        while (running) {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(SOCKET_TIMEOUT_MS);
                handle(socket);
            } catch (IOException e) {
                if (!running) {
                    return;
                }
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        HttpRequest request = HttpRequest.read(in);
        if (request == null) {
            return;
        }
        String base = "/" + token;
        if ((used || System.currentTimeMillis() > expiresAt) && (request.path.equals(base) || request.path.startsWith(base + "/"))) {
            respond(out, 410, "text/html; charset=utf-8", GONE_PAGE);
        } else if ("GET".equals(request.method) && (request.path.equals(base) || request.path.equals(base + "/"))) {
            respond(out, 200, "text/html; charset=utf-8", PAGE.replace("{token}", token).replace("{left}", Long.toString(Math.max(0, expiresAt - System.currentTimeMillis()))));
        } else if ("POST".equals(request.method) && request.path.equals(base + "/send")) {
            receive(request, in, out);
        } else {
            respond(out, 404, "text/plain; charset=utf-8", "Not found");
        }
    }

    private void receive(HttpRequest request, InputStream in, OutputStream out) throws IOException {
        long length = request.contentLength();
        if (length <= 0) {
            respond(out, 411, "text/plain; charset=utf-8", "No picture was sent");
            return;
        }
        if (length > MAX_BYTES) {
            respond(out, 413, "text/plain; charset=utf-8", "That photo is over 10 MB");
            return;
        }
        File file = new File(cacheDirectory, "phone-upload-" + System.nanoTime() + ".tmp");
        try (FileOutputStream sink = new FileOutputStream(file)) {
            byte[] buffer = new byte[16 * 1024];
            long remaining = length;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                sink.write(buffer, 0, read);
                remaining -= read;
            }
            if (remaining > 0) {
                file.delete();
                respond(out, 400, "text/plain; charset=utf-8", "The picture did not arrive in full");
                return;
            }
        }
        used = true; // the address stops working the moment a picture has been taken
        respond(out, 200, "text/plain; charset=utf-8", "ok");
        listener.onPictureReceived(file);
    }

    private static void respond(OutputStream out, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + reason(status) + "\r\n"
                + "Content-Type: " + type + "\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.write(bytes);
        out.flush();
    }

    private static String reason(int status) {
        switch (status) {
            case 200:
                return "OK";
            case 400:
                return "Bad Request";
            case 404:
                return "Not Found";
            case 411:
                return "Length Required";
            case 410:
                return "Gone";
            case 413:
                return "Payload Too Large";
            default:
                return "Error";
        }
    }

    private static String newToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            text.append(TOKEN_ALPHABET.charAt(random.nextInt(TOKEN_ALPHABET.length())));
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private static final String GONE_PAGE = "<!doctype html><html><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
            + "<body style=\"font-family:sans-serif;background:#111;color:#fff;text-align:center;padding:48px 16px\">"
            + "<h1>This Link Has Expired</h1><p style=\"color:#bbb\">Scan the new QR Code on your TV to upload another picture.</p></body></html>";

    /** What the phone sees: pick a photo, frame it at 16:9 (drag and zoom only), send it. */
    private static final String PAGE = "<!doctype html><html><head><meta charset=\"utf-8\">" + "\n"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no\"><title>Upload your Photo</title>" + "\n"
            + "<style>body{font-family:sans-serif;background:#111;color:#fff;margin:0;padding:24px 16px;text-align:center}" + "\n"
            + "h1{font-size:20px;margin:0 0 8px}p{color:#bbb;font-size:13px;margin:4px 0}" + "\n"
            + ".btn{display:inline-block;margin:12px 6px;padding:14px 26px;background:#fff;color:#111;border:0;border-radius:14px;font-size:16px;font-weight:600}" + "\n"
            + ".btn.alt{background:#333;color:#fff}input[type=file]{display:none}" + "\n"
            + "" + "\n"
            + "#crop{display:none}canvas{display:block;margin:12px auto 8px;border-radius:10px;touch-action:none;background:#000}" + "\n"
            + "@keyframes sp{to{transform:rotate(360deg)}}.spin{display:inline-block;width:20px;height:20px;border:3px solid #999;border-top-color:#111;border-radius:50%;animation:sp .8s linear infinite;vertical-align:middle}" + "\n"
            + ".tick{width:84px;height:84px;border-radius:50%;background:#2e9e5b;margin:24px auto 8px;position:relative}.tick:after{content:\"\";position:absolute;left:30px;top:16px;width:20px;height:40px;border:solid #fff;border-width:0 7px 7px 0;transform:rotate(45deg)}" + "\n"
            + "input[type=range]{width:80%;max-width:520px}#msg{margin-top:14px;font-size:13px}#msg.err{color:#ff5252}</style></head><body>" + "\n"
            + "<div id=\"head\"><h1>Upload your Photo</h1><p>(Size Limit: 10 MB)</p><p id=\"left\"></p></div>" + "\n"
            + "<div id=\"pick\"><label class=\"btn\" for=\"f\">Choose Photo</label></div>" + "\n"
            + "<input id=\"f\" type=\"file\" accept=\"image/*\">" + "\n"
            + "<div id=\"crop\"><p>Drag to Move, Pinch or use the Slider to Zoom</p>" + "\n"
            + "" + "\n"
            + "<canvas id=\"c\"></canvas><input id=\"z\" type=\"range\" min=\"0\" max=\"400\" value=\"0\">" + "\n"
            + "<div><button class=\"btn alt\" id=\"again\">Change</button><button class=\"btn\" id=\"send\">Use</button></div></div>" + "\n"
            + "<div id=\"msg\"></div>" + "\n"
            + "<script>" + "\n"
            + "var END=Date.now()+{left};" + "\n"
            + "var $=function(i){return document.getElementById(i)},f=$('f'),m=$('msg'),c=$('c'),g=c.getContext('2d'),z=$('z');" + "\n"
            + "var img,iw,ih,W,H,s,minS,x,y,ptr={},last=0,RATIO=16/9,MAX=10*1024*1024;" + "\n"
            + "(function t(){var l=Math.max(0,Math.round((END-Date.now())/1000)),e=$('left');if(!e)return;e.textContent=l>0?'Link expires in '+Math.floor(l/60)+':'+('0'+l%60).slice(-2):'This link has expired.';if(l>0)setTimeout(t,1000)})();" + "\n"
            + "function err(t){m.className='err';m.textContent=t}" + "\n"
            + "function clr(){m.className='';m.textContent=''}" + "\n"
            + "function reset(){var sb=$('send');sb.disabled=false;$('again').disabled=false;sb.textContent='Use'}" + "\n"
            + "function done(){$('crop').style.display='none';$('pick').style.display='none';$('head').style.display='none';" + "\n"
            + "var n=5;m.className='';m.innerHTML='<div class=\"tick\"></div><h1>Upload Complete</h1><p>Closing the Tab in <b id=\"n\">5</b> Secs</p>';" + "\n"
            + "var t=setInterval(function(){n--;if(n>0){$('n').textContent=n;return}clearInterval(t);" + "\n"
            + "m.innerHTML='<div class=\"tick\"></div><h1>Upload Complete</h1><p>You can close this tab now.</p>';window.close();setTimeout(function(){if(!window.closed){location.replace('about:blank')}},300)},1000)}" + "\n"
            + "function fit(){var w=Math.min(window.innerWidth-32,720,window.innerHeight*0.5*RATIO);W=Math.round(w);H=Math.round(w/RATIO);c.width=W;c.height=H}" + "\n"
            + "function clamp(){var w=iw*s,h=ih*s;x=Math.min(0,Math.max(W-w,x));y=Math.min(0,Math.max(H-h,y))}" + "\n"
            + "function draw(){clamp();g.clearRect(0,0,W,H);g.drawImage(img,x,y,iw*s,ih*s);z.value=Math.round((s/minS-1)*100)}" + "\n"
            + "function frame(){fit();minS=s=Math.max(W/iw,H/ih);x=(W-iw*s)/2;y=(H-ih*s)/2;draw()}" + "\n"
            + "function zoomAt(cx,cy,ns){ns=Math.max(minS,Math.min(minS*5,ns));x=cx-(cx-x)*ns/s;y=cy-(cy-y)*ns/s;s=ns;draw()}" + "\n"
            + "function pos(e){var r=c.getBoundingClientRect();return{x:e.clientX-r.left,y:e.clientY-r.top}}" + "\n"
            + "c.onpointerdown=function(e){c.setPointerCapture(e.pointerId);ptr[e.pointerId]=pos(e);last=0};" + "\n"
            + "c.onpointerup=c.onpointercancel=function(e){delete ptr[e.pointerId];last=0};" + "\n"
            + "c.onpointermove=function(e){if(!ptr[e.pointerId])return;var p=pos(e),o=ptr[e.pointerId],ids=Object.keys(ptr);ptr[e.pointerId]=p;" + "\n"
            + "if(ids.length==1){x+=p.x-o.x;y+=p.y-o.y;draw()}" + "\n"
            + "else if(ids.length==2){var a=ptr[ids[0]],b=ptr[ids[1]],d=Math.hypot(a.x-b.x,a.y-b.y);" + "\n"
            + "if(last){zoomAt((a.x+b.x)/2,(a.y+b.y)/2,s*d/last)}last=d}};" + "\n"
            + "c.onwheel=function(e){e.preventDefault();var p=pos(e);zoomAt(p.x,p.y,s*(e.deltaY<0?1.1:1/1.1))};" + "\n"
            + "z.oninput=function(){zoomAt(W/2,H/2,minS*(1+z.value/100))};" + "\n"
            + "f.onchange=function(){var file=f.files[0];if(!file)return;clr();img=null;" + "\n"
            + "var im=new Image();im.onload=function(){img=im;iw=im.width;ih=im.height;$('pick').style.display='none';$('head').style.display='none';$('crop').style.display='block';frame()};" + "\n"
            + "im.onerror=function(){err('That file could not be opened. Pick a photo.')};" + "\n"
            + "im.src=URL.createObjectURL(file)};" + "\n"
            + "$('again').onclick=function(){f.value='';img=null;$('crop').style.display='none';$('pick').style.display='block';$('head').style.display='block';clr()};" + "\n"
            + "function post(b){fetch('/{token}/send',{method:'POST',body:b,headers:{'Content-Type':'image/jpeg'}}).then(function(r){" + "\n"
            + "if(!r.ok){err(r.status==410?'This link has expired.':r.status==413?'That photo is over 10 MB.':'That did not work. Try another photo.');reset();return}done()})" + "\n"
            + ".catch(function(){err('Could not reach the TV. Check you are on the same Wi-Fi.');reset()})}" + "\n"
            + "$('send').onclick=function(){var sb=$('send');if(sb.disabled)return;sb.disabled=true;$('again').disabled=true;sb.innerHTML='<span class=\"spin\"></span>';clr();" + "\n"
            + "var o=document.createElement('canvas');o.width=1920;o.height=Math.round(1920/RATIO);var k=1920/W;o.getContext('2d').drawImage(img,x*k,y*k,iw*s*k,ih*s*k);" + "\n"
            + "function enc(q){o.toBlob(function(b){if(b.size>MAX&&q>0.5){enc(q-0.15);return}if(b.size>MAX){err('That photo is over 10 MB.');reset();return}post(b)},'image/jpeg',q)}enc(0.92)};" + "\n"
            + "</script></body></html>";
}
