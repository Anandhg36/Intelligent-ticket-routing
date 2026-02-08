# Intelligent AI Ticket Routing System

## Project Objectives

The Intelligent AI Ticket Routing System is designed to modernize and automate support ticket triage using responsible, explainable AI.
The primary objective is to reduce manual routing effort, improve resolution times, and ensure tickets are handled by the most relevant teams with high confidence.

Key goals:
- Minimize incorrect ticket assignments
- Accelerate issue resolution using historical knowledge
- Maintain human oversight and transparency in AI decisions
- Provide an enterprise-grade, scalable ticket routing solution

---

## Key Capabilities

- **AI-powered ticket intelligence**
  Automatically analyzes ticket subjects and descriptions using NLP to understand intent, context, and technical signals.

- **Similar ticket identification**
  Detects and surfaces historically similar tickets using semantic similarity, helping agents reuse past resolutions and reduce duplicate effort.

- **Intelligent team prediction**
  Predicts the most relevant support team and generates a confidence score for each possible team.

- **Confidence-based auto-assignment**
  Tickets are automatically assigned only when AI confidence exceeds a configurable threshold (default: **80%**), ensuring accuracy and trust.

- **Human-in-the-loop control**
  Provides a clean, intuitive dashboard where support agents can review AI predictions, similar-ticket matches, manually reassign tickets, and resolve tickets.

- **Explainable AI decisions**
  Maintains full transparency by displaying AI confidence breakdowns per team, enabling teams to understand why a ticket was routed.

---
## 🧠 Approach & Design Decisions

To automatically route support tickets to the correct team, the system requires a **reliable knowledge base** that represents how different support teams specialize in specific technical domains.

In real-world systems, this knowledge base would typically consist of **internal company documentation**, historical tickets, and runbooks. However, due to privacy and data sensitivity concerns, such internal data cannot be used in an open-source project.

For this project, I used **Kubernetes official documentation** as an open and well-structured proxy knowledge source.

### How the approach works

- Kubernetes documentation was **ingested and grouped by domain**, such as:
  - Cluster Administration  
  - Containers and Workloads  
  - Networking  
  - Scheduling and Resource Management  

- Each domain was treated as a **virtual support team**, assuming that tickets related to that documentation would naturally belong to that team.

- Technical documents within the same domain were combined to form a **team-level knowledge base**.

### Why this works

This setup allows the system to:

- Compare incoming ticket content against each team’s knowledge base  
- Predict the most relevant team with a confidence score  
- Apply the same routing logic that would be used with real internal documentation  

While the **knowledge source differs**, the **core routing logic remains identical** to a production setup. The project focuses on demonstrating how AI-driven classification, confidence-based automation, and transparent decision-making can be applied to real-world support workflows in a privacy-conscious manner.

---
## Flow chart
<img width="4980" height="1860" alt="ai_ticket_routing_architecture" src="https://github.com/user-attachments/assets/09ac8c59-0b59-4405-a978-1c7a765cb810" />

---

## System Architecture Overview

The system is composed of three primary layers:
- Frontend (Angular)
- Backend API (Spring Boot)
- AI Service (Python / FastAPI)

---

## Installation Instructions

### Prerequisites
- Java 17+
- Node.js 18+
- Python 3.10+
- Git

---

### Backend Setup
```bash
cd backend/app-service
./mvnw clean install
```

### AI Service Setup
```bash
cd backend/ticket-routing-ai
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Frontend Setup
```bash
cd frontend/ticket-routing-ai-ui
npm install
```

---

## Run Instructions

### Start Backend
```bash
./mvnw spring-boot:run
```

### Start AI Service
```bash
uvicorn app:app --reload
```

### Start Frontend
```bash
npm start
```

---

## Responsible AI

Auto-assignment is restricted to high-confidence predictions only.
All low-confidence tickets require human review.

---

## Future Enhancements
- Leverage previously resolved, similar tickets to boost confidence scores for teams that have consistently handled comparable issues.
- By logging manual ticket reassignments and using them as training signals, the system learns which routing decisions were wrong and adjusts future predictions accordingly.
- Automatically summarize customer–agent conversations after ticket closure and store resolutions for reuse when similar tickets are raised.
- Introduce AI-driven document parsing to extract structured signals from unstructured content, improving embedding quality and routing precision.
- Use successful and corrected assignments as labeled test cases to evaluate accuracy, detect model drift, and validate future improvements.
- Enable LLM-assisted solution recommendations using the same document chunks responsible for routing (implementation complete, currently disabled due to infrastructure constraints).
- Detect spikes in highly similar tickets within short time windows and proactively alert teams to identify potential regressions or release-related issues.



