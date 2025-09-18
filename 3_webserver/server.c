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

pthread_t threads[NUM_THREADS] = {0};
int client_queue[QUEUE_MAX_SIZE] = {0};
int last = QUEUE_MAX_SIZE - 1;
pthread_mutex_t lock;
bool running = true;

Queue q = {.items = {0}, .write = 0, .read = 0};


bool isEmpty(Queue* q) {
	return q->write == q->read;
}

bool isFull(Queue* q) {
	return (q->write + 1) % QUEUE_MAX_SIZE == q->read;
}

void enqueue(Queue* q, int client) {
	while (isFull(q)) {}
	
	q->items[q->write] = client;
	q->write = (q->write + 1) % QUEUE_MAX_SIZE;
}

int pop(Queue* q) {
	while (isEmpty(q)) {}
	
	int client = q->items[q->read];
	q->read = (q->read + 1) % QUEUE_MAX_SIZE;
	return client;
}


void init_thread() {
	pthread_mutex_init(&lock, NULL);
	for (int i = 0; i < NUM_THREADS; i++) {
		pthread_create(&threads[i], NULL, thread_loop, NULL); 
	}
}

void thread_teardown() {
	void *status;
	for (int i = 0; i < NUM_THREADS; i++) {
		pthread_join(threads[i], &status);
	}

	pthread_mutex_destroy(&lock);
	pthread_exit(NULL);
}

void *thread_loop() {
	while (running) {
		pthread_mutex_lock(&lock);
		int client = pop(&q);
		printf("Aquired lock and popped %d\n", client);
		pthread_mutex_unlock(&lock);
	
		response(client, "index.html");
		close(client);
		printf("Client closed: %d\n", client);
	
	}
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
        
	send(client, hdr, strlen(hdr), 0);
	
	// Send file content
	char buff[BUFFER_SIZE];
	size_t n;
	while ((n = fread(buff, 1, sizeof buff, f)) > 0) {
		send(client, buff, n, 0);
	}
	fclose(f);
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
	server_fd = create_socket();

	// Thread
	init_thread();

	printf("Listening on http://localhost:%d\n", PORT);
	struct sockaddr_in cli;
	socklen_t len = sizeof cli;

	while (running) {
		int client = accept(server_fd, (struct sockaddr*)&cli, &len);
		if (client < 0) {
			perror("accept");
			continue;
		}

		enqueue(&q, client);
	}
	
	thread_teardown();
	close(server_fd);
	return 0;
}
