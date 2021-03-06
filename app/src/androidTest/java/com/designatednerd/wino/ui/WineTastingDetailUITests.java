package com.designatednerd.wino.ui;

import android.content.Context;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.designatednerd.wino.R;
import com.designatednerd.wino.SharedPreferencesHelper;
import com.designatednerd.wino.activity.WineTastingActivity;
import com.designatednerd.wino.model.WineTasting;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.designatednerd.wino.ui.EspressoHelpers.*;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WineTastingDetailUITests {

    @Rule
    public final ActivityTestRule<WineTastingActivity> wineTastingActivity =
            new IntentsTestRule<>(WineTastingActivity.class);

    /******************
     * TEST LIFECYCLE *
     ******************/

    @Before
    public void beforeEachTest() {
        //Nuke any old state
        SharedPreferencesHelper
                .getInstance(getContext())
                .nukeAllSharedPreferences();

        //Go from the initial launch page to the details page.
        goToDetailsPage();
    }

    @After
    public void afterEachTest() {
        //Nuke any state which was just added.
        SharedPreferencesHelper
                .getInstance(getContext())
                .nukeAllSharedPreferences();
    }

    /******************
     * HELPER METHODS *
     ******************/

    private Context getContext() {
        return wineTastingActivity.getActivity();
    }

    private void goToDetailsPage() {
        //Tap the add button on the main page.
        onView(withId(R.id.add_tasting_button))
                .perform(click());
    }

    private void enterAndSaveWineInfo(String aWineName,
                                      String aVineyardName,
                                      String aVarietal,
                                      int aRating) {
        //Enter the information into text views
        enterTextIntoViewWithID(aVineyardName, R.id.tasting_detail_vineyard_name_edittext);
        enterTextIntoViewWithID(aVarietal, R.id.tasting_detail_varietal_edittext);
        enterTextIntoViewWithID(aWineName, R.id.tasting_detail_wine_name_edittext);

        //NOTE: This would do the same thing with stock edit texts, but doesn't work with
        //TextInputLayout due to this bug: https://code.google.com/p/android/issues/detail?id=178182
//        enterTextIntoViewWithHint(aVineyardName, R.string.vineyard_name);
//        enterTextIntoViewWithHint(aVarietal, R.string.wine_varietal);
//        enterTextIntoViewWithHint(aWineName, R.string.wine_name);

        //Select a rating using the spinner
        tapViewWithID(R.id.tasting_detail_rating_spinner);

        //Pull a string representing a rating from the array rather than hard-coding it
        //so you don't have to change the test when the text in the array changes.
        String[] ratings = getContext().getResources().getStringArray(R.array.ratings_array);
        String desiredRating = ratings[aRating];
        tapViewWithText(desiredRating);

        //Save the tasting
        tapViewWithText(R.string.save_tasting_title);
    }

    /*****************
     * ACTUAL TESTS! *
     *****************/

    @Test
    public void savingDataInTheDetailIsPersistedToUserDefaults() {
        //Use variables to make these easier to test against.
        String testWineName = "Test wine name";
        String testVarietal = "Cabernet Sauvignon";
        String testVineyard = "Rodney Strong";
        int testRating = 3;

        enterAndSaveWineInfo(testWineName,
                testVineyard,
                testVarietal,
                testRating);

        String expectedWineName = WineTasting.wineNameFromInfo(testWineName, testVineyard, testVarietal);

        //Check that input is saving
        List<WineTasting> savedTastings = SharedPreferencesHelper.getInstance(getContext())
                .getCurrentTastings();

        //Make sure the saved tastings only has the one which was just added.
        assertNotNull(savedTastings);
        assertEquals(savedTastings.size(), 1);

        //Grab the tasting and then verify the data matches what was entered.
        WineTasting savedTasting = savedTastings.get(0);

        assertEquals(savedTasting.vineyardName, testVineyard);
        assertEquals(savedTasting.wineName, testWineName);
        assertEquals(savedTasting.wineVarietal, testVarietal);
        assertEquals(savedTasting.rating, testRating);
        assertEquals(savedTasting.getFullWineName(), expectedWineName);

        //back out to the main view for the next test.
        pressBack();
    }

    @Test
    public void savingDataInTheDetailShowsUpInTheRecyclerView() {
        //Setup test variables
        String wineName = "RecyclerView";
        String vineyardName = "Google Vineyards";
        String varietal = "Syrah";
        int rating = 5;

        enterAndSaveWineInfo(wineName,
                vineyardName,
                varietal,
                rating);

        //back out to the main view
        pressBack();

        String expectedWineName = WineTasting.wineNameFromInfo(wineName, vineyardName, varietal);

        //The child of the view this Matcher is passed into has the appropriate title in the title view.
        Matcher<View> hasTitleChildWithAppropriateContent = withChild(allOf(withId(R.id.row_tasting_wine_name_textview),
                                                                            withText(expectedWineName)));

        String stars = "";
        for (int i = 0; i < rating; i++) {
            stars = stars + new String(Character.toChars(0x1F31F));
        }

        //The child of the view this Matcher is passed into has the appropriate content in the rating view.
        Matcher<View> hasRatingChildWithAppropriateContent = withChild(allOf(withId(R.id.row_tasting_wine_rating_textview),
                                                                             withText(stars)));

        //The child of the view this is passed into has two children that fulfill the above Matchers -
        //This is necessary because these two TextViews are wrapped in a LinearLayout.
        Matcher<View> hasChildWithBothChildren = withChild(allOf(hasTitleChildWithAppropriateContent,
                                                                 hasRatingChildWithAppropriateContent));


        //Grab the recycler view at the given index, and make sure all children match.
        onRecyclerItemViewAtPosition(R.id.wine_tasting_recyclerview, 0)
                .check(matches(hasChildWithBothChildren));
    }
}
