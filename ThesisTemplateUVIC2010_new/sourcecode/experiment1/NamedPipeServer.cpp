// Example code from: https://msdn.microsoft.com/en-us/library/windows/desktop/aa365588(v=vs.85).aspx

#include <Windows.h>
#include <stdio.h>
#include <strsafe.h>

#define BUFSIZE 512

DWORD WINAPI InstanceThread(LPVOID);
VOID GetAnswerToRequest(char *, char *, LPDWORD);

int main(VOID) {
	BOOL fConnected = FALSE;
	DWORD dwThreadId = 0;
	HANDLE hPipe = INVALID_HANDLE_VALUE, hThread = NULL;
	char *lpszPipename = "\\\\.\\pipe\\mynamedpipe";

	// The main loop creates an instance of the named pipe and
	// then waits for a client to connect to it. When the client
	// connects, a thread is created to handle communications
	// with that client, and this loop is free to wait for the
	// next client connect request.  It is an infinite loop.
	for (;;) {
		hPipe = CreateNamedPipe(
			lpszPipename,				// pipe name
			PIPE_ACCESS_DUPLEX,			// read/write access
			PIPE_TYPE_MESSAGE |			// message type pipe
			PIPE_READMODE_MESSAGE |		// message-read mode
			PIPE_WAIT,					// blocking mode
			PIPE_UNLIMITED_INSTANCES,	// max. instances
			BUFSIZE,					// output buffer size
			BUFSIZE,					// input buffer size
			0,							// client time-out
			NULL);						// default security attribute

		if (hPipe == INVALID_HANDLE_VALUE) {
			return -1;
		}

		// Wait for the client to connect; if it succeeds,
		// the function returns a nonzero value. If the function
		// returns zero, GetLastError returns ERROR_PIPE_CONNECTED.
		fConnected = ConnectNamedPipe(hPipe, NULL) ? TRUE : (GetLastError() == ERROR_PIPE_CONNECTED);

		if (fConnected) {
			// Create a thread for this client
			hThread = CreateThread(
				NULL,				// no security attribute
				0,					// default stack size
				InstanceThread,		// thread proc
				(LPVOID)hPipe,		// thread parameter
				0,					// not suspended
				&dwThreadId);		// returns thread ID

			if (hThread == NULL) {
				return -1;
			}
			else CloseHandle(hThread);
		}
		else
			// The client could not connect, so close the pipe.
			CloseHandle(hPipe);
	}
	return 0;
}

// This routine is a thread processing function to read from and reply to a client
// via the open pipe connection passed from the main loop. Note this allows
// the main loop to continue executing, potentially creating more theads of
// this procedure to run concurrently, depending on the number of incoming
// client connections.
DWORD WINAPI InstanceThread(LPVOID lpvParam) {
	HANDLE hHeap = GetProcessHeap();
	char *pchRequest = (char *)HeapAlloc(hHeap, 0, BUFSIZE);
	char *pchReply = (char *)HeapAlloc(hHeap, 0, BUFSIZE);

	DWORD cbBytesRead = 0, cbReplyBytes = 0, cbWritten = 0;
	BOOL fSuccess = FALSE;
	HANDLE hPipe = NULL;

	// Do some extra error checking since the app will keep running even if this
	// thread fails.
	if (lpvParam == NULL) {
		if (pchReply != NULL) HeapFree(hHeap, 0, pchReply);
		if (pchRequest != NULL) HeapFree(hHeap, 0, pchRequest);
		return (DWORD)-1;
	}

	if (pchRequest == NULL) {
		if (pchReply != NULL) HeapFree(hHeap, 0, pchReply);
		return (DWORD)-1;
	}

	if (pchReply == NULL) {
		if (pchRequest != NULL) HeapFree(hHeap, 0, pchRequest);
		return (DWORD)-1;
	}

	// The thread's parameter is a handle to a pipe object instance.
	hPipe = (HANDLE)lpvParam;

	// Loop until done reading
	while (1) {
		// Read client requests from the pipe.  This simplistic code only allows messages
		// up to BUFSIZE characters in length.
		fSuccess = ReadFile(
			hPipe,			// handle to pipe
			pchRequest,		// buffer to receive data
			BUFSIZE,		// size of buffer
			&cbBytesRead,	// number of bytes read
			NULL);

		if (!fSuccess || cbBytesRead == 0) {
			break;
		}

		// Process the incoming message.
		GetAnswerToRequest(pchRequest, pchReply, &cbReplyBytes);

		// Write the reply to the pipe.
		fSuccess = WriteFile(
			hPipe,			// handle to pipe
			pchReply,		// buffer to write from
			cbReplyBytes,	// number of bytes to write
			&cbWritten,		// number of bytes written
			NULL);			// not overlapped I/O

		if (!fSuccess || cbReplyBytes != cbWritten) {
			break;
		}
	}

	// Flush the pipe to allow the client to read the pipe's contents
	// before disconnecting. Then disconnect the pipe, and close the
	// handle to this pipe instance.
	FlushFileBuffers(hPipe);
	DisconnectNamedPipe(hPipe);
	CloseHandle(hPipe);

	HeapFree(hHeap, 0, pchRequest);
	HeapFree(hHeap, 0, pchReply);
	return 1;
}

// This routine is a simple function to print the client request to the console
// and populate the reply buffer with a default data string. This is where you
// would put the actual client request processing code that runs in the context
// of an instance thread. Keep in mind the main thread will continue to wait for
// and receive other client connections while the instance thread is working.
VOID GetAnswerToRequest(char *pchRequest, char *pchReply, LPDWORD pchBytes) {
	printf("Client Request String:\"%s\"\n", pchRequest);
	
	// Check the outgoing message to make sure it's not too long for the buffer.
	if (FAILED(StringCchCopy(pchReply, BUFSIZE, "This is the answer."))) {
		*pchBytes = 0;
		pchReply[0] = 0;
		return;
	}
	*pchBytes = lstrlen(pchReply) + 1;
}