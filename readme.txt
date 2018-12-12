How to use the command processor jar file:

Step 1. Set up the config.txt file

     Edit the config.txt file that specifies the location of the macro files and TCP/IP port number
     Save config.txt file to $(config location)

Step 2. On the analysis machine, start a macro running server

1) cd to the location of the jar file
   eg. cd /mnt/isilon/Henry-SPIM/workspace/
2) run the jar file from command line
   eg. java -jar command_processor.jar $(config location)

   where $(config location) is the location of the config.txt file (only path, no filename)
3) Check if the printed port number and the workspace is as expected

Step 3. Send commands to the analysis machine
   Command structure example
   if the macro script is test.ijm which have arguments arg1, arg2, arg3
   send "test arg1 arg2 arg3" to the port on the analysis server
   parse the arguments in the script


Step 4. Optional (shut down analysis engine)

   Send command "disconnect"
