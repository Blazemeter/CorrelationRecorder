---
sidebar: auto
---

# Correlation Rules

Correlation rules are the mechanism that allows you to define how to the Correlation Recorder will detect dynamic values, extract and replacement them in your recorded flow.

## Concepts

A Correlation Rule consists of three elements: a **Reference variable name**, an **Extractor**, and a **Replacement**. 

It's important to note that while the **Reference variable name must be defined,** it doesn't have to be unique. You can create **multiple** correlation rules with **the same name**.

You can create a Correlation Rule with only an Extractor or only a Replacement, but at least one of them must be defined. If you create multiple rules with the same reference variable name, but different extractors or replacements, you'll be able to extract or replace the same dynamic value in different ways.