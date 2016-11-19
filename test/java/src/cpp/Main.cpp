#include <stdio.h>

#include "Test.h"
#include "CppTestReducer.h"

int failureCount = 0;

void checkResult(int expected, int actual, const char* testname)
{
	if (expected == actual) {
		printf("Succeeded: %s\n", testname);
	} else {
		printf("FAILED: %s, expected %d != actual %d\n", testname, expected, actual);
		failureCount++;
	}
}

void checkResult(std::string expected, std::string actual, const char* testname)
{
	if (expected == actual) {
		printf("Succeeded: %s\n", testname);
	} else {
		printf("FAILED: %s, expected %s != actual %s\n", testname, expected.c_str(), actual.c_str());
		failureCount++;
	}
}

int main(int argc, char* argv[])
{
    Node* add = new Node(NodeType::Add);
    add->addChild(new Node(NodeType::IntLiteral, 1));
    add->addChild(new Node(NodeType::IntLiteral, 2));

    CppTestReducer  reducer;
    Calculator      calculator;
	reducer.label(calculator, add);
    Object result = reducer.reduce(calculator, add, Nonterminal::Int);

    checkResult(3, result.intValue, "1+2");

	Node* concat = new Node(NodeType::Concat);
	concat->addChild(new Node(NodeType::StringLiteral, "a"));
	concat->addChild(new Node(NodeType::StringLiteral, "b"));
	concat->addChild(new Node(NodeType::StringLiteral, "c"));

	reducer.label(calculator, concat);
	result = reducer.reduce(calculator, concat, Nonterminal::String);
	checkResult("abc", result.stringValue, "concat a | b | c");

	concat->addChild(new Node(NodeType::IntLiteral, 1));
	reducer.label(calculator, concat);
	result = reducer.reduce(calculator, concat, Nonterminal::String);
	checkResult("abc1", result.stringValue, "concat a | b | c | 1");

	concat->addChild(new Node(NodeType::ShortLiteral, 2));
	reducer.label(calculator, concat);
	result = reducer.reduce(calculator, concat, Nonterminal::String);
	checkResult("abc12", result.stringValue, "concat a | b | c | 1 | 2");

	Node* multiply = new Node(NodeType::Multiply);
	multiply->addChild(new Node(NodeType::IntLiteral, 2));
	multiply->addChild(new Node(NodeType::Subtract));
	multiply->getSubtree(1)->addChild(new Node(NodeType::IntLiteral, 5));
	multiply->getSubtree(1)->addChild(new Node(NodeType::IntLiteral, 2));
	reducer.label(calculator, multiply);
	result = reducer.reduce(calculator, multiply, Nonterminal::Int);
	checkResult(6, result.intValue, "2 * (5-2)");

    return failureCount;
}
