#pragma once
#include <map>
#include <string>
#include <vector>
#include "Test.h"
#include "Nonterminals.h"

struct Testcase
{
    std::string name;
    Nonterminal valueType;
    std::string expectedValue;
    Node*       root;

    Testcase(std::string name, Nonterminal type, std::string expected):
        name(name)
        , valueType(type)
        , expectedValue(expected)
        , root(NULL)
    {
    }
};

std::vector<Testcase> buildTestcases(std::string fileName);
bool isStartTag(std::string& line, std::string tagName);
bool isEndTag(std::string& line, std::string tagName);
typedef std::map<std::string,std::string> Attributes;
Attributes getAttributes(std::string& tag);
std::string toXML(Node*);
std::string nodeTypeToString(NodeType);
