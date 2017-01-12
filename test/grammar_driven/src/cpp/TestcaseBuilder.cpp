#include <fstream>
#include <iostream>
#include <sstream>
#include <stdlib.h>
#include <string>
#include <vector>

#include "Nonterminals.h"
#include "NonterminalLookup.h"
#include "Testcase.h"


static const std::string content("content");
static const std::string expected("expected");
static const std::string name("name");
static const std::string null("null");
static const std::string op("op");
static const std::string type("type");

#if TESTCASE_BUILDER_STANDALONE
int main(int argc, char* argv[])
{
    buildTestcases(argv[1]);
}
#endif

static const bool verbose = false;

#ifdef NODE_TYPE
#undef NODE_TYPE
#endif
#define NODE_TYPE(x) if (nodeType == #x) return NodeType::x;
NodeType lookupNodeType(std::string nodeType)
{
    #include "NodeTypes.inc"
    std::string diagnostic("Unrecognized nodeType ");
    diagnostic += nodeType;
    throw std::logic_error(diagnostic);
}

bool isTrue(std::string& value)
{
    return value == "yes" || value == "true" || value == "1";
}

std::vector<Testcase> buildTestcases(std::string fileName)
{
    std::vector<Testcase> result;
    std::ifstream infile(fileName);
    std::string line;

    std::vector<Node*> nodeStack;

    while (std::getline(infile, line)) {
        if (isStartTag(line, "Test")) {
        }

        if (isEndTag(line, "Test")) {
        }
        
        if (isStartTag(line, "Testcase")) {

            if (verbose) {
                std::cout << "Found Testcase: " << line << std::endl;
            }

            Attributes attrs = getAttributes(line);
            std::string canProduce = attrs["canProduce"];

            if (canProduce.empty()) {
                Nonterminal nt = NonterminalLookupByName(attrs[type]);
                result.emplace_back(Testcase(attrs[name], nt, attrs[expected]));

                if (verbose) {
                    std::cout << "\t" << "name=" << attrs[name] << " type=" << (int) nt;
                    std::cout << " expected=" << attrs[expected] << std::endl;
                }

            } else {
                Nonterminal nt = NonterminalLookupByName(canProduce);
                result.emplace_back(Testcase(attrs[name], nt, TestType::CanProduce));
            }

        }
        
        if (isEndTag(line, "Testcase")) {
        }
        
        if (isStartTag(line, "Node")) {
            Node* node;
            Attributes attrs = getAttributes(line);

            if (!attrs[op].empty()) {
                NodeType nodeType = lookupNodeType(attrs[op]);
                std::string nodeContent = attrs[content];

                if (verbose) {
                    std::cout << "Found Node: " << line << std::endl;
                    std::cout << "\t" << "op is " << (int)nodeType << std::endl;
                    if (!content.empty()) {
                        std::cout << "\t" << "content is " << nodeContent << std::endl;
                    }
                }

                if (!content.empty()) {
                    if (nodeType != NodeType::StringLiteral) {
                        node = new Node(nodeType, atoi(nodeContent.c_str()));
                    } else {
                        node = new Node(nodeType, nodeContent);
                    }
                } else {
                    node = new Node(nodeType);
                }

            } else if (isTrue(attrs[null])) {
                node = NULL;
            } else {
                std::string diagnostic("Missing required op or null attribute:");
                diagnostic += line;
                throw std::logic_error(diagnostic);
            }

            if (!nodeStack.empty()) {
                nodeStack.back()->addChild(node);
            }

            if (!isEndTag(line, "Node")) {
                nodeStack.push_back(node);
            }
        }
        
        if (isEndTag(line, "Node")) {

            if (!isStartTag(line,"Node")) {
                Node* node = nodeStack.back();
                nodeStack.pop_back();
                if (nodeStack.empty()) {
                    result.back().root = node;
                }
            }
        }
    }

    return result;
}

bool isStartTag(std::string& line, std::string tagName)
{
    size_t pos = line.find_first_not_of(" \t");

    if (pos != std::string::npos && line[pos] == '<') {
        if (line.substr(pos+1, tagName.size()) == tagName) {
            char nextC = line[pos+tagName.size()+1];
            return nextC == ' ' || nextC == '>';
        }
    }

    return false;
}

bool isEndTag(std::string& line, std::string tagName)
{
    if (isStartTag(line, tagName)) {
        return line.substr(line.size() - 2) == "/>";
    }

    size_t pos = line.find_first_not_of(" \t");

    if (pos != std::string::npos && line[pos] == '<') {
        
        if (line[pos+1] == '/')
            return (line.substr(pos+2, tagName.size()) == tagName);
    }
    return false;
}

std::map<std::string,std::string> getAttributes(std::string& tag)
{
    std::map<std::string,std::string> result;

    size_t pos = tag.find("=\"");

    while (pos != std::string::npos) {
        size_t attrNameStart = tag.rfind(" ", pos);
        size_t attrValueEnd = tag.find('"',pos+2);

        if (attrNameStart != std::string::npos && attrValueEnd != std::string::npos) {
            std::string attrName = tag.substr(attrNameStart+1, pos-1-attrNameStart);
            std::string attrValue = tag.substr(pos+2,attrValueEnd - (pos+2));
            result[attrName] = attrValue;
        } else {
            std::cerr << "funky tag attr: " << tag << ", pos = " << pos << std::endl;
            break;
        }

        pos = tag.find("=\"", attrValueEnd);
    }

    return result;
}

std::string toXML(Node* node)
{
    if (!node) {
        return "<null state=\"1\"/>";
    }

    std::string result("<");
    result += nodeTypeToString(node->getNodeType());
    result += " state=\"";
    char buff[20];
    snprintf(buff, 20, "%d", node->getStateNumber());
    result += buff;
    result += "\"";

    if (node->getSubtreeCount() > 0) {
        result += ">";

        for (size_t i = 0; i < node->getSubtreeCount(); i++) {
            result += toXML(node->getSubtree(i));
        }

        result += "</";
        result += nodeTypeToString(node->getNodeType());
        result +=">";

    } else {
        result += "/>";
    }

    return result;
}

#undef NODE_TYPE
#define NODE_TYPE(x) if (nodeType == NodeType::x) return #x;
std::string nodeTypeToString(NodeType nodeType)
{
#include "NodeTypes.inc"
return "-unknown-";
}
