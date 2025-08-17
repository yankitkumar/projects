import random
import numpy as np

rows = 10
columns = 10
actions = ['North','South','East','West','Switchon','Pickup','Return']
epsilon = 0.1
gamma = 0.9
alpha = 0.1
walls = [(5,1),(5,2),(5,3),(5,4),
         (6,1),(6,4),
         (7,4),
         (8,1),(8,2),(8,3),(8,4)]
traps = [(9,4),(5,0),(2,5)]
switch =(7,9)
gate = (7,1)
base = (0,0)
artifact = (6,2)
tunnel = [(3,9),(6,0)]
Q = np.zeros((rows,columns,2,2,len(actions)))

def greedy_epsilon(state):
    if random.uniform(0,1)<epsilon:
        return random.choice(range(len(actions)))
    else:
        return np.argmax(Q[state[0],state[1],state[2],state[3],:])

def movement(state,action):
    hero_row,hero_col,gate_status,artifact_status = state
    original_position = (hero_row,hero_col,gate_status,artifact_status)
    Done = False
    reward = -1

    if action == 0:
        if hero_row>0:
            hero_row -= 1
    elif action == 1:
        if hero_row < rows-1:
            hero_row += 1
    elif action == 2:
        if hero_col>0:
            hero_col -= 1
    elif action == 3:
        if hero_col < columns-1 :
            hero_col += 1
    elif action == 4:
        if (hero_row,hero_col) == switch and gate_status == 0:
            gate_status = 1
        else:
            reward = -10
    elif action == 5:
        if (hero_row,hero_col) == artifact and gate_status == 1 and artifact_status == 0:
            artifact_status = 1
        else:
            reward = -10
    elif action == 6:
        if (hero_row,hero_col) == base and artifact_status == 1:
            reward = 10
            Done = True
        else:
            reward = -10

    if (hero_row,hero_col) in traps:
        reward = -20

    if (hero_row,hero_col) == tunnel[0]:
        new_state = (9,0,gate_status,artifact_status)
        return new_state,reward,Done
    elif (hero_row,hero_col) == tunnel[1]:
        new_state =(0,9,gate_status,artifact_status)
        return new_state,reward,Done
    elif (hero_row,hero_col) in walls:
        new_state = original_position
        return new_state,reward,Done
    
    new_state = (hero_row, hero_col, gate_status, artifact_status)
    return new_state, reward, Done
    

def update_rule(episodes):
    for episode in range(episodes):
        hero_row,hero_col = base
        gate_status = 0
        artifact_status = 0
        state = (hero_row,hero_col,gate_status,artifact_status)
        Done = False
        while not Done:
            action = greedy_epsilon(state)
            next_state,reward,Done = movement(state,action)
            Q[state[0],state[1],state[2],state[3],action] += alpha*(reward + gamma*np.max(Q[next_state[0],next_state[1],next_state[2],next_state[3],:])-Q[state[0],state[1],state[2],state[3],action])
            state = next_state

def test_agent(start_state=None, max_steps=100):
    if start_state is None:
        hero_row, hero_col = base
        gate_status = 0
        artifact_status = 0
    else:
        hero_row, hero_col, gate_status, artifact_status = start_state

    state = (hero_row, hero_col, gate_status, artifact_status)
    steps = 0
    total_reward = 0
    

    while steps < max_steps:
        action = np.argmax(Q[state[0], state[1], state[2], state[3], :])
        action_name = actions[action]
        print(f"Step {steps}: At {state} → Action: {action_name}")

        next_state, reward, done = movement(state, action)
        total_reward += reward
        state = next_state
        steps += 1

        if done:
            print(f"\n Agent successfully returned to base with artifact in {steps} steps! ")
            print(f" Total reward: {total_reward}")
            break
    else:
        print("\n Agent failed to complete the mission within step limit.")
        print(f" Final state: {state} |  Total reward: {total_reward}")

update_rule(50000)  
test_agent()        

