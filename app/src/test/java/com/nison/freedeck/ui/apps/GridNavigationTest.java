package com.nison.freedeck.ui.apps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GridNavigationTest {

    private static final int NONE = GridNavigation.NONE;

    // ---- right ----

    @Test
    public void rightMovesToTheNextApp() {
        assertEquals(1, GridNavigation.afterRight(0, 8));
        assertEquals(4, GridNavigation.afterRight(3, 8));
    }

    @Test
    public void rightFromTheEndOfARowContinuesOnTheNextRow() {
        assertEquals(5, GridNavigation.afterRight(4, 8)); // 5 columns: card 4 ends row 1, card 5 starts row 2
    }

    @Test
    public void rightFromTheLastAppGoesBackToTheFirst() {
        assertEquals(0, GridNavigation.afterRight(7, 8));
    }

    // ---- left ----

    @Test
    public void leftMovesToThePreviousApp() {
        assertEquals(0, GridNavigation.afterLeft(1, 8));
        assertEquals(6, GridNavigation.afterLeft(7, 8));
    }

    @Test
    public void leftFromTheStartOfARowContinuesOnTheRowAbove() {
        assertEquals(4, GridNavigation.afterLeft(5, 8));
    }

    @Test
    public void leftFromTheFirstAppGoesToTheLast() {
        assertEquals(7, GridNavigation.afterLeft(0, 8));
    }

    // ---- never leaves the cards ----

    @Test
    public void everyCardHasATargetInBothDirections() {
        for (int count = 1; count <= 12; count++) {
            for (int position = 0; position < count; position++) {
                int right = GridNavigation.afterRight(position, count);
                int left = GridNavigation.afterLeft(position, count);
                assertEquals("right from " + position + " of " + count, true, right >= 0 && right < count);
                assertEquals("left from " + position + " of " + count, true, left >= 0 && left < count);
            }
        }
    }

    @Test
    public void rightThenLeftComesBackToWhereYouWere() {
        for (int count = 1; count <= 12; count++) {
            for (int position = 0; position < count; position++) {
                assertEquals(position, GridNavigation.afterLeft(GridNavigation.afterRight(position, count), count));
            }
        }
    }

    @Test
    public void goingRightThroughEveryAppVisitsEachOnceAndComesBack() {
        int count = 7;
        boolean[] seen = new boolean[count];
        int position = 0;
        for (int step = 0; step < count; step++) {
            assertEquals(false, seen[position]);
            seen[position] = true;
            position = GridNavigation.afterRight(position, count);
        }
        assertEquals(0, position);
    }

    @Test
    public void aSingleAppStaysWhereItIs() {
        assertEquals(0, GridNavigation.afterRight(0, 1));
        assertEquals(0, GridNavigation.afterLeft(0, 1));
    }

    @Test
    public void invalidPositionsAreIgnored() {
        assertEquals(NONE, GridNavigation.afterRight(-1, 8));
        assertEquals(NONE, GridNavigation.afterLeft(-1, 8));
        assertEquals(NONE, GridNavigation.afterRight(8, 8));
        assertEquals(NONE, GridNavigation.afterLeft(8, 8));
    }

    @Test
    public void anEmptyListIgnoresBothKeys() {
        assertEquals(NONE, GridNavigation.afterRight(0, 0));
        assertEquals(NONE, GridNavigation.afterLeft(0, 0));
    }

    // 14 cards in 5 columns: rows of 5, 5 and 4.

    @Test
    public void downMovesToTheCardBelow() {
        assertEquals(7, GridNavigation.afterDown(2, 14, 5));
    }

    @Test
    public void downFromTheLastRowWrapsToTheTopOfTheSameColumn() {
        assertEquals(3, GridNavigation.afterDown(13, 14, 5));
        assertEquals(0, GridNavigation.afterDown(10, 14, 5));
    }

    @Test
    public void downIntoAShortLastRowGoesToTheLastCard() {
        assertEquals(13, GridNavigation.afterDown(9, 14, 5));
    }

    @Test
    public void aSingleRowDownStaysInItsColumn() {
        assertEquals(2, GridNavigation.afterDown(2, 5, 5));
    }

    @Test
    public void downIgnoresWhatIsNotACard() {
        assertEquals(GridNavigation.NONE, GridNavigation.afterDown(-1, 14, 5));
    }

    @Test
    public void wrappingUpMovesToTheCardAbove() {
        assertEquals(2, GridNavigation.afterUpWrapping(7, 14, 5));
    }

    @Test
    public void wrappingUpFromTheTopRowGoesToTheBottomOfTheSameColumn() {
        assertEquals(10, GridNavigation.afterUpWrapping(0, 14, 5));
        assertEquals(13, GridNavigation.afterUpWrapping(3, 14, 5));
    }

    @Test
    public void wrappingUpIntoAShortLastRowGoesToTheLastCard() {
        assertEquals(13, GridNavigation.afterUpWrapping(4, 14, 5));
    }

    @Test
    public void twoColumnsWalkEveryCardInBothDirections() {
        // 5 cards in 2 columns: rows (0 1) (2 3) (4)
        assertEquals(2, GridNavigation.afterDown(0, 5, 2));
        assertEquals(4, GridNavigation.afterDown(2, 5, 2));
        assertEquals(0, GridNavigation.afterDown(4, 5, 2));
        assertEquals(4, GridNavigation.afterDown(3, 5, 2));   // nothing below it: the last card
        assertEquals(3, GridNavigation.afterDown(1, 5, 2));
        assertEquals(4, GridNavigation.afterUpWrapping(0, 5, 2));
        assertEquals(4, GridNavigation.afterUpWrapping(1, 5, 2));
        assertEquals(0, GridNavigation.afterUpWrapping(2, 5, 2));
    }
}
