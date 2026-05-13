export function buildNotificationShim(initialState: string): string {
  return `
    (function() {
      if (window.__aionuiNotificationPermissionInstalled) {
        window.__aionuiNotificationPermissionState = '${initialState}';
        return;
      }
      window.__aionuiNotificationPermissionInstalled = true;
      window.__aionuiNotificationPermissionState = '${initialState}';
      function requestPermission(callback) {
        var action = Promise.resolve(window.__aionuiNotificationPermissionState);
        if (window.AionUiHarmonyBridge && window.AionUiHarmonyBridge.requestNotificationPermission) {
          action = Promise.resolve(window.AionUiHarmonyBridge.requestNotificationPermission()).then(function(state) {
            window.__aionuiNotificationPermissionState = state || 'default';
            return window.__aionuiNotificationPermissionState;
          }).catch(function() {
            return 'denied';
          });
        }
        return action.then(function(state) {
          if (typeof callback === 'function') callback(state);
          return state;
        });
      }
      if (!window.Notification) {
        window.Notification = function Notification() {};
      }
      try {
        Object.defineProperty(window.Notification, 'permission', {
          configurable: true,
          get: function() { return window.__aionuiNotificationPermissionState || 'default'; }
        });
      } catch (error) {
        window.Notification.permission = window.__aionuiNotificationPermissionState || 'default';
      }
      window.Notification.requestPermission = requestPermission;
    })();
  `;
}
