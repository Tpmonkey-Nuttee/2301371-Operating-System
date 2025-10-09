// WARNING: I don't even know what the teacher want us to code...
// this is me guessing what he wants us to do...

#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <time.h>
#include <stdlib.h>
#include <stdbool.h>

#define READER_COUNT 990
#define WRITER_COUNT 10
#define TOTAL_THREADS READER_COUNT + WRITER_COUNT

int x = 0;

sem_t w_mutex;
sem_t r_mutex;
sem_t mutex;
int read_count = 0;

bool in_array(int *arr, int num, int size) {
	for (size_t i = 0; i < size; i++) {
		if (arr[i] == num) {
			return true;
		}
	}
	return false;
}

int compare( const void* a, const void* b)
{
     int int_a = * ( (int*) a );
     int int_b = * ( (int*) b );

     if ( int_a == int_b ) return 0;
     else if ( int_a < int_b ) return -1;
     else return 1;
}

void sample(int *arr, int count, int max) {
	int random;
	srand(time(NULL));
	for (int i = 0; i < count; i++) {
		do {
			random = rand() % max;
		} while (in_array(arr, random, count));
		arr[i] = random;
	}
	qsort(arr, count, sizeof(int), compare);
}

void wait_a_sec() {
	time_t start, now;
	time(&start);
	time(&now);
	
	// what in the fuck did i cook
	for (;now < start + 1; time(&now)) {}
}


void *writer(void *arg) {
	int self = *((int *) arg);
	sem_wait(&w_mutex);
	sem_wait(&r_mutex);

	wait_a_sec();
	x++;
	printf("no = %-3i x = %i\n", self, x);

	sem_post(&w_mutex);
	sem_post(&r_mutex);
}


void *reader() {
	sem_wait(&w_mutex);
	sem_post(&w_mutex);

	sem_wait(&mutex);
	++read_count;
	if (read_count == 1) {sem_wait(&r_mutex);}
	sem_post(&mutex);

	wait_a_sec();
//	printf("Read  x = %i\n", x);
	
	sem_wait(&mutex);
	--read_count;
	if (read_count == 0) {sem_post(&r_mutex);}
	sem_post(&mutex);
	
}


int main() {
	pthread_t threads[TOTAL_THREADS];
	time_t start_t, end_t;
	int *ptr;
	
	sem_init(&w_mutex, 0, 1);
	sem_init(&r_mutex, 0, 1);
	sem_init(&mutex, 0, 1);

	// random writers
	int random_n[WRITER_COUNT];
	sample(random_n, WRITER_COUNT, TOTAL_THREADS);
	int curr = 0;

	time(&start_t);	

	for (int i = 0; i < TOTAL_THREADS; i++) {
		if (i == random_n[curr]) {
			pthread_create(&threads[i], NULL, writer, &random_n[curr]);
			curr++;
		}
		else {
			pthread_create(&threads[i], NULL, reader, NULL);
		}
	}
	
	
	for (int i = 0; i < TOTAL_THREADS; i++) {
		pthread_join(threads[i], NULL);
	}

	time(&end_t);

	printf("Finished in %li seconds.\n", end_t - start_t);

	sem_destroy(&w_mutex);
	sem_destroy(&r_mutex);
	sem_destroy(&mutex);
	return 0;
}
