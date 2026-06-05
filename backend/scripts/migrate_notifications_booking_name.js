// Run this with the Mongo shell (mongo) or MongoDB CLI (mongosh)
// Example: mongosh "<your-connection-string>" ./migrate_notifications_booking_name.js

const ObjectId = require('mongodb').ObjectId; // not needed in mongosh, but works in Node

// Script logic for mongosh (works with mongosh):
// For each notification with category BOOKING_STATUS, fetch booking by referenceId,
// then use booking.resourceName or resource.name to build a friendly message.

const cursor = db.notifications.find({ category: 'BOOKING_STATUS' });
let count = 0;
while (cursor.hasNext()) {
  const n = cursor.next();
  const refId = n.referenceId;
  if (!refId) continue;
  const booking = db.bookings.findOne({ _id: ObjectId(refId) });
  let resourceLabel = 'resource';
  if (booking) {
    if (booking.resourceName && booking.resourceName.trim() !== '') {
      resourceLabel = booking.resourceName;
    } else if (booking.resourceId) {
      try {
        const res = db.resources.findOne({ _id: ObjectId(booking.resourceId) });
        if (res && res.name && res.name.trim() !== '') {
          resourceLabel = res.name;
        } else {
          resourceLabel = booking.resourceId;
        }
      } catch (e) {
        resourceLabel = booking.resourceId;
      }
    }
  }

  // Determine status text from title (e.g., "Booking APPROVED") or existing message
  let statusWord = '';
  if (n.title) {
    const parts = n.title.split(' ');
    statusWord = parts[parts.length - 1];
  }
  if (!statusWord && n.message) {
    // try to extract approved/rejected from message
    const m = n.message.toLowerCase();
    if (m.indexOf('approved') >= 0) statusWord = 'approved';
    if (m.indexOf('rejected') >= 0) statusWord = 'rejected';
    if (m.indexOf('cancel') >= 0) statusWord = 'cancelled';
  }
  const statusLower = statusWord ? String(statusWord).toLowerCase() : '';

  let newMsg = `Your booking for ${resourceLabel} was ${statusLower}.`;
  // preserve reason if present on booking or original message
  if (booking && booking.decisionReason) {
    newMsg += ` Reason: ${booking.decisionReason}`;
  } else if (n.message && n.message.includes('Reason:')) {
    // copy existing reason text
    const idx = n.message.indexOf('Reason:');
    newMsg += ' ' + n.message.substring(idx);
  }

  db.notifications.updateOne({ _id: n._id }, { $set: { message: newMsg } });
  count++;
}
print(`Updated ${count} booking notifications.`);
