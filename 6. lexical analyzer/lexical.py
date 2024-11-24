import re

# Lists to store different categories of lexemes
keywords = ["int", "return", "if", "else", "for", "while"]
operators = ["+", "-", "*", "/", "=", "<", ">"]
separators = ["(", ")", "{", "}", ",", ";"]

# Dynamic list for identifiers
identifiers = []

# Function to find the index of the lexeme in the corresponding list
def find_index(lexeme, array):
    try:
        return array.index(lexeme)
    except ValueError:
        return -1  # Return -1 if the lexeme is not found

# Function to check if an identifier is already in the identifiers list
def find_identifier(lexeme):
    return lexeme in identifiers

# Function to add a new identifier to the list
def add_identifier(lexeme):
    if not find_identifier(lexeme):
        identifiers.append(lexeme)

# Function to classify and print lexemes
def lex_analyze(input_file):
    try:
        with open(input_file, "r") as file:
            line_number = 0

            for line in file:
                line_number += 1
                tokens = re.split(r'\s+', line.strip())  # Tokenize by whitespace

                for token in tokens:
                    # Check for keywords
                    index = find_index(token, keywords)
                    if index != -1:
                        print(f"Line {line_number}: {token} \t Token: KEYWORD \t Token Value (Index): {index}")
                    # Check for operators
                    elif find_index(token, operators) != -1:
                        index = find_index(token, operators)
                        print(f"Line {line_number}: {token} \t Token: OPERATOR \t Token Value (Index): {index}")
                    # Check for separators
                    elif find_index(token, separators) != -1:
                        index = find_index(token, separators)
                        print(f"Line {line_number}: {token} \t Token: SEPARATOR \t Token Value (Index): {index}")
                    # Check for numbers
                    elif token.isdigit():
                        print(f"Line {line_number}: {token} \t Token: NUMBER \t Token Value: {token}")
                    # Check for identifiers
                    elif re.match(r'^[A-Za-z_]\w*$', token):
                        add_identifier(token)  # Add identifier to the list if it’s new
                        identifier_index = find_index(token, identifiers)
                        print(f"Line {line_number}: {token} \t Token: IDENTIFIER \t Token Value (Index): {identifier_index}")
                    else:
                        print(f"Error on line {line_number}: Invalid token '{token}'")
    except FileNotFoundError:
        print("Error opening file")

# Function to print the list of identifiers
def print_identifiers():
    print("\nIdentifiers Table:")
    for i, identifier in enumerate(identifiers):
        print(f"Identifier {i}: {identifier}")

# Main function
def main():
    # Input file name
    input_file = "C:\\1VU\\TY\\System Programming\\SPL Practice\\6. lexical analyzer\\input.txt"

    # Call lexical analyzer function
    lex_analyze(input_file)

    # Print the list of identifiers
    print_identifiers()

if __name__ == "__main__":
    main()
