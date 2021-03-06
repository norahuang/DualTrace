// Example code from: https://msdn.microsoft.com/en-us/library/windows/desktop/aa365592(v=vs.85).aspx

#include <Windows.h>
#include <stdio.h>
#include <conio.h>

#define BUFSIZE 512

int main(int argc, char *argv[]) {
	HANDLE hPipe;
	char* lpvMessage = "This is a test.";
	char chBuf[BUFSIZE];
	BOOL fSuccess = FALSE;
	DWORD cbRead, cbToWrite, cbWritten, dwMode;
	char* lpszPipename = "\\\\.\\pipe\\mynamedpipe";

	if (argc > 1)
		lpvMessage = argv[1];

	// Try to open a named pipe; wait for it, if necessary.
	while (1) {
		hPipe = CreateFile(
			lpszPipename,	// pipe name
			GENERIC_READ |	// read and write access
			GENERIC_WRITE,
			0,				// no sharing
			NULL,			// default security attributes
			OPEN_EXISTING,	// opens existing pipe
			0,				// default attributes
			NULL);			// no template file

		// Break if the pipe handle is valid.
		if (hPipe != INVALID_HANDLE_VALUE)
			break;

		// Exit if an error other than ERROR_PIPE_BUSY occurs.
		if (GetLastError() != ERROR_PIPE_BUSY) {
			return -1;
		}

		// All pipe instances are busy, so wait for 20 seconds.
		if (!WaitNamedPipe(lpszPipename, 20000)) {
			return -1;
		}
	}

	// The pipe connected; change to message-read mode.
	dwMode = PIPE_READMODE_MESSAGE;
	fSuccess = SetNamedPipeHandleState(
		hPipe,		// pipe handle
		&dwMode,	// new pipe mode
		NULL,		// don't set maximum bytes
		NULL);		// don't set maximum time

	if (!fSuccess) {
		return -1;
	}

	// Send a message to the pipe server.
	cbToWrite = (lstrlen(lpvMessage) + 1);

	fSuccess = WriteFile(
		hPipe,			// pipe handle
		lpvMessage,		// message
		cbToWrite,		// message length
		&cbWritten,		// bytes written
		NULL);			// not overlapped

	if (!fSuccess) {
		return -1;
	}

	do {
		// Read from the pipe.
		fSuccess = ReadFile(
			hPipe,		// pipe handle
			chBuf,		// buffer to receive reply
			BUFSIZE,	// size of buffer
			&cbRead,	// number of bytes read
			NULL);

		if (!fSuccess && GetLastError() != ERROR_MORE_DATA)
			break;

	} while (!fSuccess);  // repeat loop if ERROR_MORE_DATA

	if (!fSuccess) {
		return -1;
	}

	getch();
	CloseHandle(hPipe);

	return 0;
}