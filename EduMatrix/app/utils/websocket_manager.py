from fastapi import WebSocket
from typing import Dict, List

class ConnectionManager:
    def __init__(self):
        # group_id -> {user_id -> List[WebSocket]}
        self.active_connections: Dict[str, Dict[str, List[WebSocket]]] = {}

    async def connect(self, websocket: WebSocket, group_id: str, user_id: str, accept: bool = True):
        if accept:
            try:
                await websocket.accept()
            except RuntimeError:
                pass # Already accepted
        
        if group_id not in self.active_connections:
            self.active_connections[group_id] = {}
        
        if user_id not in self.active_connections[group_id]:
            self.active_connections[group_id][user_id] = []
            
        if websocket not in self.active_connections[group_id][user_id]:
            self.active_connections[group_id][user_id].append(websocket)
        # print(f"User {user_id} connected to group {group_id}")

    def disconnect(self, websocket: WebSocket, group_id: str, user_id: str):
        if group_id in self.active_connections:
            if user_id in self.active_connections[group_id]:
                if websocket in self.active_connections[group_id][user_id]:
                    self.active_connections[group_id][user_id].remove(websocket)
                
                if not self.active_connections[group_id][user_id]:
                    del self.active_connections[group_id][user_id]
            
            if not self.active_connections[group_id]:
                del self.active_connections[group_id]
        # print(f"User {user_id} disconnected from group {group_id}")

    def remove_socket_from_all(self, websocket: WebSocket, user_id: str):
        groups_to_clean = []
        for group_id, users in self.active_connections.items():
            if user_id in users and websocket in users[user_id]:
                users[user_id].remove(websocket)
                if not users[user_id]:
                    del users[user_id]
                if not users:
                    groups_to_clean.append(group_id)
        
        for group_id in groups_to_clean:
            del self.active_connections[group_id]
        # print(f"User {user_id} fully disconnected from global websocket")

    async def broadcast_to_group(self, message: dict, group_id: str):
        """Broadcasts a message to all connected users in the group."""
        if group_id in self.active_connections:
            dead_sockets = []
            for user_id, sockets in self.active_connections[group_id].items():
                for socket in sockets:
                    try:
                        await socket.send_json(message)
                    except Exception as e:
                        print(f"Error sending message to {user_id}: {e}")
                        dead_sockets.append((user_id, socket))
            
            for uid, sock in dead_sockets:
                self.disconnect(sock, group_id, uid)

manager = ConnectionManager()
