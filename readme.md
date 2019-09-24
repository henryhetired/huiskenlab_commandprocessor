# Huisken lab command processor user guide
## Overview
The huisken lab command processor runs on ```_huisken-analysis-1_``` to **headlessly** runs specific commands. Users can interface with the ```command processor server``` by sending commands via **TCP/IP**. 

Since the server takes commands from **TCP/IP** connection, it is up to the user to write scripts to send the commands in sequence or in batch. 
## Server Installation
The ```command processor server``` is written in ```Java``` (:roll_eyes:) in order to allow easier access to the ```ImageJ``` library. If you want to create your own server, simply download the entire ```repo``` and import as a project into ```intellij```, build the ```command_processor.jar``` file and edit the ```config.txt``` file per your network configurations. Contact henryhe5692wow@gmail.com for help if you run into issues.

## Command structures
There are two typical use case for the ```command processor server```:
1. You need to run commands during acquisition.
    - Stream data to the analysis machine and save to the file server.
    - Generate __MIP__ on-the-fly.
    - Other potentially "__Smart__" applications
1. You have existing ```Fiji``` macros and ```python``` scripts written and want to off-load the analysis pressure.
