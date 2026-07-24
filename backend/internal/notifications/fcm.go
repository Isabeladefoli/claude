// Package notifications cuida de enviar notificações push via FCM.
package notifications

import (
	"context"
	"database/sql"
	"fmt"
	"log"

	"firebase.google.com/go/v4/messaging"
)

// FCMNotifier envia notificações push aos usuários via Firebase Cloud Messaging.
type FCMNotifier struct {
	db  *sql.DB
	fcm *messaging.Client
}

// New cria um notificador FCM. Se fcm for nil, notificações são logadas só.
func New(db *sql.DB, fcm *messaging.Client) *FCMNotifier {
	return &FCMNotifier{db: db, fcm: fcm}
}

// NotifyNewMessage envia notificação de nova mensagem para um usuário.
// Busca todos os device tokens dele e envia via FCM.
func (n *FCMNotifier) NotifyNewMessage(ctx context.Context, userID int64, senderName string) error {
	// Se não tem FCM configurado, só loga (desenvolvimento).
	if n.fcm == nil {
		log.Printf("[notif-mock] Mensagem de %s -> usuário %d", senderName, userID)
		return nil
	}

	// Busca todos os device tokens do usuário.
	rows, err := n.db.QueryContext(ctx,
		`SELECT token FROM device_tokens WHERE user_id = ?`,
		userID,
	)
	if err != nil {
		return fmt.Errorf("buscando tokens: %w", err)
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var token string
		if err := rows.Scan(&token); err != nil {
			continue
		}
		tokens = append(tokens, token)
	}

	if len(tokens) == 0 {
		// Usuário não tem nenhum device registrado ou não liga notificações.
		return nil
	}

	// Envia pra todos os devices dele.
	message := &messaging.MulticastMessage{
		Tokens: tokens,
		Notification: &messaging.Notification{
			Title: "Private Messenger",
			Body:  fmt.Sprintf("Mensagem de %s", senderName),
		},
		// Android: faz vibrar e toca som
		Android: &messaging.AndroidConfig{
			Priority: "high",
			Notification: &messaging.AndroidNotification{
				Sound:     "default",
				ChannelID: "messages",
			},
		},
		// iOS: silent se desligado, senão toca
		APNS: &messaging.APNSConfig{
			Payload: &messaging.APNSPayload{
				Aps: &messaging.APS{
					Sound: "default",
				},
			},
		},
	}

	resp, err := n.fcm.SendMulticast(ctx, message)
	if err != nil {
		return fmt.Errorf("enviando via FCM: %w", err)
	}

	// Log simples de sucesso/falha.
	if resp.SuccessCount > 0 {
		log.Printf("[fcm] %d notificações enviadas ao usuário %d", resp.SuccessCount, userID)
	}
	if resp.FailureCount > 0 {
		log.Printf("[fcm] %d falhas ao notificar usuário %d", resp.FailureCount, userID)
	}

	return nil
}
