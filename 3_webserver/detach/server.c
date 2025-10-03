#include "server.h"

#include <stdio.h>
#include <stdbool.h>
#include <signal.h>
#include <sys/socket.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>


int server_fd;
pthread_mutex_t lock;
pthread_attr_t attr;


void init_thread() {
	pthread_mutex_init(&lock, NULL);
	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
}

void thread_teardown() {
	pthread_attr_destroy(&attr);
	pthread_mutex_destroy(&lock);
}

void response(int client, const char *path) {
	// Get file size for content length
	int size;
	FILE *f = fopen(path, "r");
	if (!f) {
		perror("File Open");
		return;
	}
	
	fseek(f, 0, SEEK_END);
	size = ftell(f);

	// Set file pointer back to 0
	fseek(f, 0, SEEK_SET);
	
	// Send header
	char hdr[MAX_HDR_SIZE];
	sprintf(
			hdr,
		       	"HTTP/1.1 200 OK\r\n"
			"Content-Type: text/html\r\n"
			"Connection: close\r\n"
			"Content-Length: %d\r\n"
			"\r\n",
			size
	);
        

	 send(client, hdr, strlen(hdr), MSG_NOSIGNAL);
	
	// Send file content
	char buff[BUFFER_SIZE];
	size_t n;
	while ((n = fread(buff, 1, sizeof buff, f)) > 0) {
		send(client, buff, n, MSG_NOSIGNAL);
	}
	fclose(f);
}

void *handle_request(void *client_void) {
	int client = *((int *) client_void);
	pthread_mutex_unlock(&lock);
	
	response(client, "index.html");

//	char buffer[BUFFER_SIZE];	
//	int readBytes = recv(client, buffer, BUFFER_SIZE, 0);

	close(client);
	printf("Handled client: %i\n", client);	
	
	pthread_exit(NULL);
}

int create_socket() {
	int server_fd = socket(AF_INET, SOCK_STREAM, 0);
	if (server_fd < 0) {
		perror("socket");
		exit(1);
	}

	// wtf is an easy restart
	int opt = 1;
	setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

	struct sockaddr_in addr = {0};
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = INADDR_ANY;
	addr.sin_port= htons(PORT);

	if (bind(server_fd, (struct sockaddr*)&addr, sizeof addr) < 0) {
		perror("bind");
		close(server_fd);
		exit(1);
	}
	
	if (listen(server_fd, 5) < 0) {
		perror("listen");
		close(server_fd);
		exit(1);
	}

	return server_fd;
}
	
int main() {
	int rc;
	void *client_ptr;
	pthread_t thread;

	server_fd = create_socket();

	init_thread();
	
	printf("Server is running at PID %i\n", getpid());
	printf("Listening on http://localhost:%d\n", PORT);
	
	struct sockaddr_in cli;
	socklen_t len = sizeof cli;

	for  (;;) {
		int client = accept(server_fd, (struct sockaddr*)&cli, &len);
		if (client < 0) {
			perror("accept");
			continue;
		}
		printf("Accepted Client: %i\n", client);
		
		pthread_mutex_lock(&lock);		
		client_ptr = &client;
		rc = pthread_create(&thread, &attr, handle_request, client_ptr);
		if (rc) {
			perror("Create thread");
			goto end;
		}
		if (!pthread_detach(thread)) {
			perror("Detach thread");
			goto end;
		}
	}
	
end:
	thread_teardown();
	close(server_fd);
	return 0;
}
