#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

// Arrays to store different categories of lexemes
const char* keywords[] = { "int", "return", "if", "else", "for", "while" };
const char* operators[] = { "+", "-", "*", "/", "=", "<", ">" };
const char* separators[] = { "(", ")", "{", "}", ",", ";" };

// Dynamic array for identifiers
char** identifiers = NULL;
int identifierCount = 0;

// Function to find the index of the lexeme in the corresponding array
int findIndex(const char* array[], int size, const char* lexeme) {
    for (int i = 0; i < size; i++) {
        if (strcmp(array[i], lexeme) == 0) {
            return i;
        }
    }
    return -1;  // Return -1 if the lexeme is not found
}

// Function to check if an identifier is already in the identifiers array
int findIdentifier(const char* lexeme) {
    for (int i = 0; i < identifierCount; i++) {
        if (strcmp(identifiers[i], lexeme) == 0) {
            return 1;  // Identifier found
        }
    }
    return 0;  // Identifier not found
}

// Function to add a new identifier to the dynamic array
void addIdentifier(const char* lexeme) {
    if (!findIdentifier(lexeme)) {
        identifiers = realloc(identifiers, (identifierCount + 1) * sizeof(char*));
        identifiers[identifierCount] = malloc(strlen(lexeme) + 1);
        strcpy(identifiers[identifierCount], lexeme);
        identifierCount++;
    }
}

// Function to classify and print lexemes
void lexAnalyze(const char* inputFile) {
    FILE* file = fopen(inputFile, "r");
    if (!file) {
        perror("Error opening file");
        return;
    }

    char line[256];
    int lineNumber = 0;

    while (fgets(line, sizeof(line), file)) {
        lineNumber++;
        char* token = strtok(line, " \t\n");

        while (token != NULL) {
            if (findIndex(keywords, sizeof(keywords) / sizeof(keywords[0]), token) != -1) {
                printf("Line %d: %s \t Token: KEYWORD \t Token Value (Index): %d\n", 
                       lineNumber, token, findIndex(keywords, sizeof(keywords) / sizeof(keywords[0]), token));
            } 
            else if (findIndex(operators, sizeof(operators) / sizeof(operators[0]), token) != -1) {
                printf("Line %d: %s \t Token: OPERATOR \t Token Value (Index): %d\n", 
                       lineNumber, token, findIndex(operators, sizeof(operators) / sizeof(operators[0]), token));
            } 
            else if (findIndex(separators, sizeof(separators) / sizeof(separators[0]), token) != -1) {
                printf("Line %d: %s \t Token: SEPARATOR \t Token Value (Index): %d\n", 
                       lineNumber, token, findIndex(separators, sizeof(separators) / sizeof(separators[0]), token));
            } 
            else if (isdigit(token[0])) {
                printf("Line %d: %s \t Token: NUMBER \t Token Value: %s\n", lineNumber, token, token);
            } 
            else if (isalpha(token[0]) || token[0] == '_') {
                printf("Line %d: %s \t Token: IDENTIFIER \t Token Value: N/A\n", lineNumber, token);
                addIdentifier(token);  // Add identifier to the dynamic array if it’s new
            } 
            else {
                printf("Error on line %d: Invalid token '%s'\n", lineNumber, token);
            }
            token = strtok(NULL, " \t\n");
        }
    }
    fclose(file);
}

// Function to free the dynamic array memory for identifiers
void freeIdentifiers() {
    for (int i = 0; i < identifierCount; i++) {
        free(identifiers[i]);
    }
    free(identifiers);
}

// Main function
int main() {
    // Input file name
    const char* inputFile = "input.c";

    // Call lexical analyzer function
    lexAnalyze(inputFile);

    // Print the list of identifiers
    printf("\nIdentifiers Table:\n");
    for (int i = 0; i < identifierCount; i++) {
        printf("Identifier %d: %s\n", i, identifiers[i]);
    }

    // Free allocated memory
    freeIdentifiers();

    return 0;
}
