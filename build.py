#!/usr/bin/env python3 

##########################################################
# Function:                                              #
#   Compiles project                                     #
#                                                        #
# How to use;                                            #
#   python3 build.py | [ARGS]                            #    
#                                                        #
# ARGS:                                                  #
#                                                        #
#   Default:                                             #
#       NONE                                             #
#           Features:                                    #
#               [X] Blackjack binary located in ./bin    #
#   Clean:                                               #       
#       -c                                               #   
#           Features:                                    #   
#               [X] Removes all binaries                 #
#               [X] Removes CMake Build directories      #   
#                                                        #
#                                                        #
#   Debug:                                               #
#       -d                                               #
#           Features:                                    #
#               [X] Blackjack binary located in ./bin    #
#               [X] Debug statements throughout runtime  #
#                                                        #
#   Server:                                              #
#       -s                                               # 
#           Feature:                                     #
#               [X] Server binary located in ./bin       #
#   Help:                                                #
#       -h                                               #
#           Features:                                    #
#               [X] Prints help menu                     #
##########################################################

import subprocess
import sys 
import os 

def ensure_build_dir():
    if not os.path.exists("./build/"):
        os.makedirs("build")

def print_help_menu():
     print("""
    Usage:
           ./build.py | [ARGS]
    
    Arguments:
           None     Production Binary 
           -c       Clean Project 
           -d       Debug Binary 
           -h       Help Menu
           """)

def make_build_dir():
    cmd = "mkdir build"
    print(f"Running: {cmd}")
    subprocess.run(cmd.split())

def make_clean():
    cmd = "sudo rm -rf ./build ./bin"
    print(f"Running: {cmd}")
    subprocess.run(cmd.split())

def make_prod():
        make_clean()
        make_build_dir()

        cmd = "cmake .. -DCMAKE_BUILD_TYPE=Release && cmake --build ."
        print(f"Running: cd build/ && {cmd}")
        subprocess.run(cmd, shell=True, cwd="./build")

def make_debug():
    make_clean()
    make_build_dir()

    cmd = "cmake -DCMAKE_BUILD_TYPE=Debug .. && cmake --build ."
    print(f"Running: cd build/ && {cmd}")
    subprocess.run(cmd, shell=True, cwd="./build")

def make_tests():
    make_clean()
    make_build_dir()
    
    cmd = "cmake -DCMAKE_BUILD_TYPE=Debug -DBUILD_TESTS=ON .. && cmake --build ."
    print(f"Running: cd build/ && {cmd}")
    subprocess.run(cmd, shell=True, cwd="./build")
    
    # Run tests
    cmd = "../bin/test/BlackjackTests"
    print(f"Running tests: {cmd}")
    subprocess.run(cmd, shell=True, cwd="./build")

def run(version):
    cmd = f"./bin/{version}/Blackjack"
    print(f"Running: {cmd}")
    subprocess.run(cmd.split())

def main():
    if len(sys.argv) == 1:
        make_prod()
        run("release")    
    else:
        if(sys.argv[1] == "-c"):
            make_clean()
        elif(sys.argv[1] == "-d"):
            make_debug()
            run("debug")
#        elif(sys.argv[1] == "-s"):
#            make_and_run_server()
        elif(sys.argv[1] == "-t"):
            make_tests()
        else:
            print_help_menu()

    
if __name__ == "__main__":
    main()
