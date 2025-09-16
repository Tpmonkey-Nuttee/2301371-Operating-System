#include <stdio.h>
#include <sys/socket.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <unistd.h>
#include <threads.h>

#define PORT 8080
#define BUFFER_SIZE 1024
#define NUM_THREADS 8
#define MAX_HDR_SIZE 512


int get_file_size(const char *path) {
	// TODO: don't open file twice. but fuck this for now
	int size;
	FILE *f = fopen(path, "r");
	fseek(f, 0, SEEK_END);
	size = ftell(f);
	fclose(f);
	return size;
}

void send_ok_header(int client, int size) {
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
        
	send(client, hdr, strlen(hdr), 0);
}


void send_file(int client, const char *path) {
	FILE *f = fopen(path, "r");
	if (!f) {
		perror("open html");
		return;
	}

	char buff[BUFFER_SIZE];
	size_t n;
	while ((n = fread(buff, 1, sizeof buff, f)) > 0) {
		send(client, buff, n, 0);
	}
	fclose(f);
}


void accept_client_thread(int* client) {
	printf("Accept client %d\n", *client);
	
	int size = get_file_size("index.html");
	send_ok_header(*client, size);	
	send_file(*client, "index.html");
	
	close(*client);
	printf("Client closed %d\n", *client);
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
	int server_fd = create_socket();

	// Thread
	pthread_t threads[NUM_THREADS];
	// TODO: THREADING AAAAA

	printf("Listening on http://localhost:%d\n", PORT);
	struct sockaddr_in cli;
	socklen_t len = sizeof cli;

	for (;;) {
		int client = accept(server_fd, (struct sockaddr*)&cli, &len);
		if (client < 0) {
			perror("accept");
			continue;
		}
		accept_client_thread(&client);

	}
	
	close(server_fd);
	return 0;
}
