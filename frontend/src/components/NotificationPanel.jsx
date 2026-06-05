import { useCallback, useEffect, useRef, useState } from 'react'
import campusApi from '../api/campusApi'
import { CheckCircle, Bell } from 'lucide-react'

export default function NotificationPanel({
  sectionId = 'notifications',
  title = 'Notifications',
  subtitle = 'Stay updated on booking and ticket changes.',
  refreshInterval = 15000
}) {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const isMountedRef = useRef(true)

  const loadNotifications = useCallback(async ({ silent = false } = {}) => {
    try {
      if (!silent) {
        setLoading(true)
      }
      setError('')
      const response = await campusApi.get('/notifications')
      if (!isMountedRef.current) return
      setNotifications(response.data || [])
    } catch (err) {
      if (!isMountedRef.current) return
      setError('Failed to load notifications.')
      console.error(err)
    } finally {
      if (!isMountedRef.current) return
      if (!silent) {
        setLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    isMountedRef.current = true
    loadNotifications()

    let intervalId
    if (refreshInterval > 0) {
      intervalId = window.setInterval(() => {
        loadNotifications({ silent: true })
      }, refreshInterval)
    }

    return () => {
      isMountedRef.current = false
      if (intervalId) {
        window.clearInterval(intervalId)
      }
    }
  }, [loadNotifications, refreshInterval])

  const handleMarkAsRead = async (id) => {
    try {
      await campusApi.put(`/notifications/${id}/read`)
      setNotifications(notifications.map(n => 
        n.id === id ? { ...n, isRead: true } : n
      ))
    } catch (err) {
      console.error('Failed to mark notification as read', err)
    }
  }

  const handleMarkAllAsRead = async () => {
    try {
      await campusApi.put('/notifications/read-all')
      setNotifications(notifications.map(n => ({ ...n, isRead: true })))
    } catch (err) {
      console.error('Failed to mark all notifications as read', err)
    }
  }

  if (loading) {
    return (
      <section id={sectionId} className="user-section admin-panel-box glass-panel-soft">
        <div className="panel-top">
          <div>
            <h2>{title}</h2>
            <p>{subtitle}</p>
          </div>
        </div>
        <div className="empty-state custom-empty glass-empty">
          <span className="empty-icon">⏳</span>
          <p>Loading notifications...</p>
        </div>
      </section>
    )
  }

  const unreadCount = notifications.filter(n => !n.isRead).length

  return (
    <section id={sectionId} className="user-section admin-panel-box glass-panel-soft">
      <div className="panel-top">
        <div>
          <h2>{title}</h2>
          <p>{subtitle}</p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllAsRead}
            className="btn-action btn-primary"
            style={{
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              fontSize: '0.875rem',
              fontWeight: '500'
            }}
          >
            Mark all as read
          </button>
        )}
      </div>

      {error && (
        <div style={{ color: '#e11d48', padding: '12px', marginBottom: '12px' }}>
          {error}
        </div>
      )}

      {notifications.length === 0 ? (
        <div className="empty-state custom-empty glass-empty">
          <span className="empty-icon">🔔</span>
          <p>No notifications yet.</p>
        </div>
      ) : (
        <div className="alert-list" style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
          {notifications.map((notification) => (
            <div
              key={notification.id}
              className="alert-item"
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '12px',
                borderRadius: '8px',
                backgroundColor: notification.isRead ? 'transparent' : 'rgba(59, 130, 246, 0.1)',
                borderLeft: notification.isRead ? '3px solid #cbd5e1' : '3px solid #3b82f6',
                border: notification.isRead ? '1px solid #e2e8f0' : 'none'
              }}
            >
              <div style={{ flex: 1 }}>
                <p style={{ margin: '0 0 4px 0', fontWeight: notification.isRead ? '400' : '600' }}>
                  {notification.title || "Notification"}
                </p>
                {notification.message && (
                  <p style={{ margin: 0, fontSize: '0.875rem', color: '#64748b' }}>
                    {notification.message}
                  </p>
                )}
                {notification.createdAt && (
                  <p style={{ margin: '4px 0 0 0', fontSize: '0.75rem', color: '#94a3b8' }}>
                    {new Date(notification.createdAt).toLocaleString()}
                  </p>
                )}
              </div>
              {!notification.isRead && (
                <button
                  onClick={() => handleMarkAsRead(notification.id)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: '8px',
                    marginLeft: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px',
                    color: '#3b82f6',
                    fontSize: '0.875rem'
                  }}
                  title="Mark as read"
                >
                  <CheckCircle size={18} />
                </button>
              )}
              {notification.isRead && (
                <Bell size={18} style={{ color: '#94a3b8', marginLeft: '12px' }} />
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}