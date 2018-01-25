@echo off
start "Server" NamedPipeServerOverlapped.exe

start "Client 1" NamedPipeClient.exe "Message 1"
start "Client 2" NamedPipeClient.exe "Message 2"
start "Client 3" NamedPipeClient.exe "Message 3"
start "Client 4" NamedPipeClient.exe "Message 4"

