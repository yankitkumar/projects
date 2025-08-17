# Q-Learning Hunting Agent 

This is a custom reinforcement learning environment inspired by the classical grid-world problem, where an agent must:
- Navigate a maze with **walls**, **traps**, and **tunnels**
- Activate a **switch** to open a **gate**
- Pick up an **artifact** and return it to **base**

The agent is trained using **Q-Learning** with an `epsilon-greedy` strategy.

## Features
- Custom rewards and penalties
- 7 discrete actions (move, pickup, return, switch)
- Special states like teleportation tunnels and switch-based gates

## To run simply do :
```bash
python hunt.py
```
## Expected Output 
```bash
Step 0: At (0, 0, 0, 0) → Action: South

Agent successfully returned to base with artifact in 41 steps!
Total reward: -30
