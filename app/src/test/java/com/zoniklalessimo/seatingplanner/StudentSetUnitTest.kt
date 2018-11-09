package com.zoniklalessimo.seatingplanner

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class StudentSetUnitTest {
    @get:Rule
    val rule = ActivityTestRule(EditStudentSetActivity::class.java)

    @Test
    fun runThrough_StudentSet_Interactions() {

        val name = "Annie"

        for (i in 0..3) {
            onView(withId(R.id.add_student)).perform(click())
            // Give student a name
            onView(
                    withChild(withText("")))
                    .perform(typeText(name + i.toString()))
        }
        onData(withParentIndex(0)).onChildView(withId(R.id.neighbour_count)).perform(click())
        onData(withText(name)).perform(click())
    }
}