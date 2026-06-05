# Document Canonicalization and PDF Generation POC

## Overview

This project demonstrates a format-agnostic document ingestion architecture designed for AI/RAG workloads.

Instead of performing direct document-to-PDF conversion, documents are first transformed into a canonical representation and then rendered into a semantic PDF.

Current implementation supports:

* DOCX → Canonical Model → PDF

Planned support:

* XLSX
* PPTX
* EML
* MSG

---

# Problem Statement

The organization's existing ingestion pipeline supports PDF chunking and vectorization.

Enterprise knowledge, however, exists in multiple formats:

* DOCX
* XLSX
* PPTX
* EML
* MSG

A direct conversion approach would require building and maintaining independent conversion pipelines for each format.

```text
DOCX → PDF
XLSX → PDF
PPTX → PDF
EML  → PDF
MSG  → PDF
```

This leads to duplicated logic, inconsistent output quality, and higher maintenance costs.

---

# Proposed Architecture

```text
                ┌─────────────┐
                │   DOCX      │
                ├─────────────┤
                │   XLSX      │
                ├─────────────┤
                │   PPTX      │
                ├─────────────┤
                │   EML       │
                ├─────────────┤
                │   MSG       │
                └──────┬──────┘
                       │
                       ▼
          ┌─────────────────────────┐
          │ Canonical Document Model│
          └───────────┬─────────────┘
                      │
                      ▼
             ┌────────────────┐
             │ PDF Generator  │
             └───────┬────────┘
                     │
                     ▼
                PDF Chunker
                     │
                     ▼
                 Vector DB
```

---

# Why Use a Canonical Model?

## Traditional Conversion Optimizes for Visual Fidelity

Direct document conversion attempts to preserve:

* Fonts
* Colors
* Margins
* Alignment
* Layout
* Shapes
* Rendering details

Example:

```text
DOCX
 ↓
PDF
```

The objective is:

> Make the PDF look identical to the original document.

This approach is valuable for:

* Legal archiving
* Compliance retention
* Human consumption

However, AI systems do not consume visual layout.

---

## AI/RAG Systems Optimize for Semantic Fidelity

Embedding models and retrieval systems care about:

* Headings
* Paragraphs
* Lists
* Tables
* Images
* Metadata

They do not benefit significantly from:

* Font families
* Margins
* Colors
* Precise positioning
* Visual styling

The canonical model preserves semantic structure rather than visual structure.

Example:

```json
{
  "type": "heading",
  "level": 1,
  "text": "Quarterly Revenue"
}
```

This representation is significantly more valuable for retrieval than:

```text
Font: Arial
Size: 18
Color: Blue
Position: x=100 y=250
```

---

# Architectural Benefits

## 1. Format Agnostic Processing

Without a canonical model:

```text
DOCX → PDF
XLSX → PDF
PPTX → PDF
EML  → PDF
MSG  → PDF
```

With a canonical model:

```text
DOCX ┐
XLSX │
PPTX │
EML  │
MSG  │
      ▼
Canonical Model
      ▼
PDF Renderer
```

Only the extraction layer is format-specific.

Rendering remains shared.

---

## 2. Better Retrieval Quality

The chunker receives clean semantic content:

```text
Heading

Paragraph

TABLE

Quarter | Revenue
Q1      | 100
Q2      | 115
```

This improves:

* Chunk quality
* Embedding quality
* Retrieval accuracy

---

## 3. Future-Proof Design

Current flow:

```text
Canonical Model
      ↓
PDF
      ↓
Chunker
      ↓
Vector DB
```

Future flow:

```text
Canonical Model
      ↓
Direct Chunking
      ↓
Embeddings
      ↓
Vector DB
```

The extraction layer remains unchanged.

Only downstream adapters evolve.

---

## 4. Easier Testing

Extraction can be validated independently.

Example:

```text
DOCX
 ↓
Canonical Model
```

can be inspected directly as JSON.

This eliminates PDF rendering as a debugging dependency.

---

## 5. Better Table Preservation

Tables are frequently degraded during document conversion.

Canonical representation preserves structure explicitly:

```json
{
  "type": "table",
  "headers": [
    "Quarter",
    "Revenue"
  ],
  "rows": [
    ["Q1", "100"],
    ["Q2", "115"]
  ]
}
```

This structure is significantly more useful for AI retrieval.

---

## 6. Reduced Vendor Dependency

Traditional rendering often relies on:

* Microsoft Office
* LibreOffice
* Proprietary rendering engines

Canonical extraction relies primarily on:

* Apache POI
* Jakarta Mail
* PDFBox

Resulting in:

* Lower licensing risk
* Better portability
* Simpler deployment

---

# Canonical Document Model

Current supported block types:

```text
heading
paragraph
list_item
table
image
metadata
attachment
```

All supported document formats should map into these common block types.

Example:

DOCX Heading:

```json
{
  "type": "heading"
}
```

PPT Slide Title:

```json
{
  "type": "heading"
}
```

Email Subject:

```json
{
  "type": "heading"
}
```

The renderer does not need to know the original source format.

---

# Current Implementation

Implemented:

* DOCX Extraction
* Canonical Model Generation
* Semantic PDF Rendering
* Multi-page PDF Support
* Text Wrapping
* Table Rendering
* Image Placeholders

---

# Current Assumptions

The current implementation intentionally optimizes for:

```text
Semantic Fidelity > Visual Fidelity
```

Assumptions:

1. PDF is an intermediate artifact.
2. Retrieval quality is more important than visual appearance.
3. Tables are rendered as structured text.
4. Images are represented as placeholders.
5. Reading order is more important than page layout.
6. Formatting metadata is not required for retrieval.

---

# Future Enhancements

## Extraction

* XLSX Extractor
* PPTX Extractor
* EML Extractor
* MSG Extractor
* Hyperlink Extraction
* Inline Image Extraction
* Text Box Support
* SmartArt Support

## Rendering

* Actual Image Embedding
* Table Formatting Improvements
* Metadata Rendering
* Attachment Rendering
* OCR Support

## AI Enhancements

* Direct Chunking
* Metadata-Aware Chunking
* Table-Aware Chunking
* Multimodal Retrieval

---

# Key Takeaway

This architecture shifts document processing from:

```text
Document Rendering
```

to:

```text
Information Extraction
```

For AI and RAG systems, preserving document meaning is significantly more valuable than preserving document appearance.
