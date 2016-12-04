#pragma once

#include <cstddef>
#include <vector>
#include <string>
#include <map>
#include <stdio.h>

enum class NodeType { 
    Add,
    AddStrict,
    Concat,
    IntLiteral,
    Multiply,
    QualifiedLiteral,
    ShortLiteral,
    StringLiteral,
    Subtract,
};

class Node
{
public:
    Node(NodeType type): type(type), leaf(NULL), intValue(-1) {}
    Node(NodeType type, int intValue): type(type), leaf(NULL), intValue(intValue) {}
    Node(NodeType type, const char* str): type(type), intValue(-1), stringValue(str) {}

    NodeType getNodeType() const
    {
        return type;
    }

    Node& addChild(Node* child)
    {
        children.push_back(child);
        return *this;
    }

    size_t getSubtreeCount() const
    {
        return children.size();
    }

    Node* getSubtree(size_t index)
    {
        return children[index];
    }

    int getStateNumber() const
    {
        return stateNumber;
    }

    void setStateNumber(int stateNumber)
    {
        this->stateNumber = stateNumber;
    }

    void setTransitionTableLeaf(void* leaf)
    {
        this->leaf = leaf;
    }

    void* getTransitionTableLeaf() const
    {
        return leaf;
    }

    int getIntValue() const
    {
        return intValue;
    }

    const char* getStringValue() const
    {
        return stringValue.c_str();
    }

private:
    NodeType            type;
    std::vector<Node*>  children;
    void*               leaf;
    int                 intValue;
    std::string         stringValue;
    int                 stateNumber;
};

struct Object
{
    int         intValue;
    std::string stringValue;
};

class Calculator
{
public:
    /*
     * Terminals
     */
    Object intLiteral(Node* node)
    {
        Object result;
        result.intValue = node->getIntValue();
        return result;
    }

    Object shortLiteral(Node* node)
    {
        Object result;
        result.intValue = node->getIntValue();
        return result;
    }

    Object stringLiteral(Node* node)
    {
        Object result;
        result.stringValue = node->getStringValue();
        return result;
    }

    /*
     * Unary arithmetic operators
     */
    Object negate(Node* node, Object o)
    {
        Object result;
        result.intValue = -(o.intValue);
        return result;
    }

    Object identity(Node* node, Object o)
    {
        return o;
    }

    /*
     * Binary arithmetic operators
     */
    Object add(Node* node, Object lhs, Object rhs)
    {
        Object result;
        result.intValue = lhs.intValue + rhs.intValue;
        return result;
    }

    Object multiply(Node* node, Object lhs, Object rhs)
    {
        Object result;
        result.intValue = lhs.intValue * rhs.intValue;
        return result;
    }

    Object subtract(Node* node, Object lhs, Object rhs)
    {
        Object result;
        result.intValue = lhs.intValue - rhs.intValue;
        return result;
    }

    Object divide(Node* node, Object lhs, Object rhs)
    {
        Object result;
        result.intValue = lhs.intValue / rhs.intValue;
        return result;
    }

    Object concatFixed(Node* node, Object rhs, Object lhs)
    {
        Object result;
        result.stringValue = lhs.stringValue;
        result.stringValue += rhs.stringValue;
        return result;
    }

    /*
     * Ternary arithmetic operators
     */
    Object addTernary(Node* node, Object a, Object b, Object c)
    {
        Object result;
        result.intValue = a.intValue + b.intValue + c.intValue;
        return result;
    }

    /*
     * Variadic operators
     */
    Object concat(Node*, std::vector<Object> args)
    {
        Object result;
        for (auto& x: args) {
            result.stringValue += x.stringValue;
        }
        return result;
    }

    /*
     * Conversion operators
     */
    Object convertToString(Node* node, Object v)
    {
        Object result;
        // Not all host compilers have std::to_string()
        result.stringValue = intToString(v.intValue);
		return result;
    }

    Object widenShortToInt(Node* node, Object o)
    {
        // All signed fixed-points stored as ints, noop
        return o;
    }

    /*
     * Predicates
     */
    bool shortGuard(Node* node)
    {
        return node->getIntValue() >= -32768 && node->getIntValue() <= 32767;
    }

private:
    std::string intToString(const int i)
    {
        char buf[64];
        snprintf(buf, 64, "%d",i);
        return std::string(buf);
    }

};

