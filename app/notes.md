#Uses of native view tag:
    In function spawnTable() in TableScene.kt, inside the onLongClickListener, when the table was movable, 
    thenew tables are given the tag, "drag_disabled_once" to ensure no strange interactions occur
    between the new and old tables. When the drag is ended, those tags are reset to null in OnTableDragListener.kt
    
#TODO:
    You can make StudentSet in core package to be an abstract class

    Displace a table not only at sideOptions but on all sides

    When a view was displaced at a side, the indicator keeps indicating until the shadow has left the indicator.
    However it should disappear as soon as the shadow is within the scene bounds again.