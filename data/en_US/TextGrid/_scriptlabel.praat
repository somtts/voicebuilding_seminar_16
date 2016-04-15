select Strings listOfFiles
numberOfFiles = Get number of strings
for index to numberOfFiles
	select Strings listOfFiles
	filename$ = Get string... index
	Read from file... 'filename$'
	newobject = Extract tier... 1
	Save as Xwaves label file... text/'filename$'
	Remove
endfor