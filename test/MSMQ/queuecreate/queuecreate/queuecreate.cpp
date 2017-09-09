#include "stdafx.h"
#include "windows.h"  
#include "mq.h"  
#include "tchar.h"  
#define BUFLEN 256

#pragma comment(lib,"mqrt.lib")

HRESULT CreateMSMQQueue(
	LPWSTR wszPathName,
	PSECURITY_DESCRIPTOR pSecurityDescriptor,
	LPWSTR wszOutFormatName,
	DWORD *pdwOutFormatNameLength
)
{

	// Define the maximum number of queue properties.  
	const int NUMBEROFPROPERTIES = 2;

	// Define a queue property structure and the structures needed to initialize it.  
	MQQUEUEPROPS   QueueProps;
	MQPROPVARIANT  aQueuePropVar[NUMBEROFPROPERTIES];
	QUEUEPROPID    aQueuePropId[NUMBEROFPROPERTIES];
	HRESULT        aQueueStatus[NUMBEROFPROPERTIES];
	HRESULT        hr = MQ_OK;

	// Validate the input parameters.  
	if (wszPathName == NULL || wszOutFormatName == NULL || pdwOutFormatNameLength == NULL)
	{
		return MQ_ERROR_INVALID_PARAMETER;
	}

	// Set queue properties.  
	DWORD cPropId = 0;
	aQueuePropId[cPropId] = PROPID_Q_PATHNAME;
	aQueuePropVar[cPropId].vt = VT_LPWSTR;
	aQueuePropVar[cPropId].pwszVal = wszPathName;
	cPropId++;

	WCHAR wszLabel[MQ_MAX_Q_LABEL_LEN] = L"Test Queue";
	aQueuePropId[cPropId] = PROPID_Q_LABEL;
	aQueuePropVar[cPropId].vt = VT_LPWSTR;
	aQueuePropVar[cPropId].pwszVal = wszLabel;
	cPropId++;

	// Initialize the MQQUEUEPROPS structure.  
	QueueProps.cProp = cPropId;               // Number of properties  
	QueueProps.aPropID = aQueuePropId;        // IDs of the queue properties  
	QueueProps.aPropVar = aQueuePropVar;      // Values of the queue properties  
	QueueProps.aStatus = aQueueStatus;        // Pointer to the return status  

											  // Call MQCreateQueue to create the queue.  
	WCHAR wszFormatNameBuffer[BUFLEN];
	DWORD dwFormatNameBufferLength = BUFLEN;
	hr = MQCreateQueue(pSecurityDescriptor,         // Security descriptor  
		&QueueProps,                 // Address of queue property structure  
		wszFormatNameBuffer,         // Pointer to format name buffer  
		&dwFormatNameBufferLength);  // Pointer to receive the queue's format name length in Unicode characters not bytes.  

									 // Return the format name if the queue is created successfully.  
	if (hr == MQ_OK || hr == MQ_INFORMATION_PROPERTY)
	{
		if (*pdwOutFormatNameLength >= dwFormatNameBufferLength)
		{
			wcsncpy_s(wszOutFormatName, *pdwOutFormatNameLength - 1, wszFormatNameBuffer, _TRUNCATE);
			// ************************************  
			// You must copy wszFormatNameBuffer into the   
			// wszOutFormatName buffer.  
			// ************************************  
			wszOutFormatName[*pdwOutFormatNameLength - 1] = L'\0';
			*pdwOutFormatNameLength = dwFormatNameBufferLength;
		}
		else
		{
			printf("The queue was created, but its format name cannot be returned.\n");
		}
	}
	return hr;
}

int main(int argc, _TCHAR* argv[])
{
	wchar_t name[] = L".\\PRIVATE$\\VinayQueue21";
	DWORD bufferLength = 256;
	wchar_t formattedQueueName[256];
	printf("program is running\n");
    fflush(stdout);

	HRESULT returnValue = CreateMSMQQueue(name, NULL, formattedQueueName, &bufferLength);

	if (returnValue != MQ_OK)
		printf("Creating a Queue failed\n");
	else
	{
		printf("Queue was successfully created..Formatted QueueName =%s\n", formattedQueueName);
		printf("LEn returned is %d\n", bufferLength);
	}
	fflush(stdout);
	getchar();

}