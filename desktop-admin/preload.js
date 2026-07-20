const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('managerDesktop', {
  getBootstrap: () => ipcRenderer.invoke('auth:bootstrap'),
  login: (password) => ipcRenderer.invoke('auth:login', password),
  openExternal: (url) => ipcRenderer.invoke('app:open-external', url),
  apiRequest: (action, data) => ipcRenderer.invoke('api:request', { action, data })
});
