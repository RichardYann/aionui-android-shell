import notificationManager from '@ohos.notificationManager'

export class NotificationBridge {
  async getPermissionState(): Promise<string> {
    try {
      const enabled = await notificationManager.isNotificationEnabled()
      return enabled ? 'granted' : 'default'
    } catch (_error) {
      return 'default'
    }
  }

  async requestPermission(): Promise<string> {
    try {
      const enabled = await notificationManager.isNotificationEnabled()
      return enabled ? 'granted' : 'default'
    } catch (_error) {
      return 'denied'
    }
  }
}

export class NotificationProxy {
  private bridge: NotificationBridge

  constructor(bridge: NotificationBridge) {
    this.bridge = bridge
  }

  async requestNotificationPermission(): Promise<string> {
    return this.bridge.requestPermission()
  }
}
