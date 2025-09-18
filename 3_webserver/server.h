#include <stdbool.h>

#define PORT 8080
#define BUFFER_SIZE 1024
#define NUM_THREADS 8
#define MAX_HDR_SIZE 512
#define QUEUE_MAX_SIZE 128

typedef struct {
	int items[QUEUE_MAX_SIZE];
	int write;
	int read;
} Queue;

bool isEmpty(Queue* q);
bool isFull(Queue* q);
void enqueue(Queue* q, int client);
int pop(Queue* q);


void init_thread();

void thread_teardown();

// void wait_insert_queue(int client);
// int pop_queue();

void *thread_loop();

void response(int client, const char *path);

int create_socket();
