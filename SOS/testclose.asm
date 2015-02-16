#This program tries to close a device that is not open.
#This should hopefully cause an error and exit the program.

#close the console device
SET r4 1       #keyboard device id
PUSH r4        #push device number 1 (console output)
SET r4 4       #CLOSE sys call id
PUSH r4        #push the sys call id onto the stack
TRAP           #close the device


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

#exit syscall
:exit
SET  r4 0      #EXIT system call id
PUSH r4        #push sys call id on stack
TRAP           #exit the program
