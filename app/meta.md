## JSON Notation

# Table
{
    "x": 0.34 | 0-1
    "y": 0.56 | 0-1
    "seats": 6 | Seat Count as Number
    "separators": \[
        2,
        4
    \] | Array of Numbers
}

# TablePlan
[
    "name": "room_10b"
    // Tables
]

# Entry
Format: name|src|3-digits-rows 3-digits-seats
Example: Old Room␟old_room_plan.txt␟004015

Reminder:
ChooseTableEntry in ChoosePlanDialogViewModel.kt: Due to hardcoding in findInstance(), the src argument
    should remain the second in oder
Though you must still look over the method again if you wish to add an argument, again due to hardcoding