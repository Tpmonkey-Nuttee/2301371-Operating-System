// WARNING: I don't even know what the teacher want us to code...
// this is me guessing what he wants us to do...

#include <stdio.h>
#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <time.h>
#include <stdlib.h>


int x = 0;
sem_t w_mutex;
sem_t r_mutex;
sem_t mutex;
int read_count = 0;

void wait_a_sec() {
	time_t start, now;
	time(&start);
	time(&now);
	
	// what in the fuck did i cook
	for (;now < start + 1; time(&now)) {}
}


void *writer() {
	sem_wait(&w_mutex);
	wait_a_sec();
	x++;
	printf("Write x = %i\n", x);
	sem_post(&w_mutex);
}


void *reader() {
<<<<<<< HEAD

	sem_wait(&r_mutex);
	++read_count;
	if (read_count == 1) {sem_wait(&w_mutex);}
	sem_post(&r_mutex);

	
	// This is a critical section
	wait_a_sec();
	printf("Read  x = %i\n", x);
	// -----------
	
	sem_wait(&r_mutex);
	--read_count;
	if (read_count == 0) {sem_post(&w_mutex);}
	sem_post(&r_mutex);
	
=======
	wait_a_sec();
	sem_wait(&w_mutex); // ?????
	printf("Read  x = %i\n", x);
	sem_post(&w_mutex);
>>>>>>> 9e8dee459dbb48152cc3b4efcbd45543467a1ace
}


int main() {
	pthread_t threads[1000];
	time_t start, end;
	
	sem_init(&w_mutex, 0, 1);
	sem_init(&r_mutex, 0, 1);
	sem_init(&mutex, 0, 99);

	time(&start);
	
	for (int i = 0; i < 1000; i++) {
		if (i % 100 == 0) { // ah yes, "randomize" a writer lol
			pthread_create(&threads[i], NULL, writer, NULL);
		}
		else {
			pthread_create(&threads[i], NULL, reader, NULL);
		}
	}


	for (int i = 0; i < 1000; i++) {
		pthread_join(threads[i], NULL);
	}

	time(&end);

	printf("Finished in %li seconds.\n", end-start);

	sem_destroy(&w_mutex);
	return 0;
}
