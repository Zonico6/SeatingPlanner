# What's on your Todo list? (partial):
TODOs:  
	- Add big dividers to EmptyTableView for highlighting several tables in a row
	- Make sure that when starting a drag on a table it doesn't get centered at the finger but rather stays the same position and this is handled correctly when releasing the table
	- Maybe add boolean option to EmptyTableView that causes every divider to be drawn and separators only between those dividers instead of replacing them
	- Deal with table sizes
	- The normal division with '/' doesn't produce floating point numbers. Match a regex with ' / ' on all files and determine on every match if it's ok that the division of int / int produces always also int
	
Btw you can use setRotation, getRotation, etc. of ConstraintLayout to handle rotation (probably)