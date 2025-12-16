// Simple dataflow test

function readUserInput() {
  // Simulate untrusted user input (source)
  return "alert('hacked!')";
}

function processInput(input) {
  // Just pass it through (intermediate function)
  return input.trim();
}

function main() {
  const userInput = readUserInput();
  const cleanedInput = processInput(userInput);

  // Dangerous sink (eval)
  eval(cleanedInput);
}

main();
