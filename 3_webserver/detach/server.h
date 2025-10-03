#include <stdbool.h>

#define PORT 8080
#define BUFFER_SIZE 1024
#define MAX_HDR_SIZE 256

void init_thread();

void thread_teardown();

// void wait_insert_queue(int client);
// int pop_queue();

void *handle_request(void *client);

void response(int client, const char *path);

int create_socket();
