package com.zoniklalessimo.seatingplanner

import androidx.test.espresso.Espresso.onData
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4

import androidx.test.espresso.action.ViewActions.*


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