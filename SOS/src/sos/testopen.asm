#This program combines print10.asm and readwrite.asm.
#It'll print the numbers 1-10 to the console, then print 10 random keystrokes.
#Authors: Well, Nuxoll. But Micah and Nathan combined them, and it's for our assignment!
#Reserve the console device
SET r0 1       #device #1 (console output)
PUSH r0        #push device id on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#Reserve the keyboard device
SET r0 0       #device #0 (keyboard)
PUSH r0        #push argument on stack
SET r4 3       #OPEN sys call id
PUSH r4        #push sys call id on stack
TRAP           #open the device

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#Initialize the variables
SET r1 0       #counter
SET r2 1       #increment amount
SET r3 10      #limit

#begin loop
:loop
ADD r1 r2 r1

#print the current value in the count to the console
SET r4 1       #device id 1 = console
PUSH r4        #push device number
PUSH r0        #push address (arg not used by this device so any val will do)
PUSH r1        #push value to send to device
SET r4 6       #WRITE system call id
PUSH r4        #push the sys call id
TRAP           #system call to print the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Success code
BNE r0 r4 exit #exit program on error

#end of loop
BNE r1 r3 loop #repeat 10 times


#Initialize the variables
SET r1 0       #counter
SET r2 1       #increment amount
SET r3 10      #limit

#Main Loop
:loop2
ADD r1 r2 r1

#Read a keystroke from the keyboard
SET r0 0       #device #0 (keyboard)
PUSH r0        #push device number
PUSH r0        #push address (arg not used by this device so any val will do)
SET r0 5       #READ system call
PUSH r0        #push system call id
TRAP           #system call to read the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#save the keystroke
POP r4         #save the value in r4

#Write the value to the console
SET r0 1       #device #1 (console output)
PUSH r0        #push device number
PUSH r0        #push address (arg not used by this device so any val will do)
PUSH r4        #push value to send to device
SET r0 6       #WRITE system call
PUSH r0        #push system call id
TRAP           #system call to write the value

#Check for failure
POP r4         #get return code from the system call
SET r0 0       #Succes code
BNE r0 r4 exit #exit program on error

#loop test
BNE r1 r3 loop2

#close the console device
SET r0 1
PUSH r0        #push device number 1 (console output)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device

#exit syscall
:exit
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program
