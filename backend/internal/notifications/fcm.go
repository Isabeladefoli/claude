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
		// Usuário não tem nenhum device registrado. Sem token, não há pra onde
		// enviar — provavelmente o destinatário não fez login desde que o
		// registro de token passou a existir, ou o registro falhou no app.
		log.Printf("[fcm] usuário %d não tem device token registrado — nada enviado", userID)
		return nil
	}

	log.Printf("[fcm] enviando notificação para usuário %d (%d device(s))", userID, len(tokens))

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
				Aps: &messaging.Aps{
					Sound: "default",
				},
			},
		},
	}

	// SendEachForMulticast envia cada mensagem individualmente pela API HTTP v1.
	// (O antigo SendMulticast usava o endpoint /batch, que o Google desativou em
	// 2024 e agora responde 404.)
	resp, err := n.fcm.SendEachForMulticast(ctx, message)
	if err != nil {
		log.Printf("[fcm] ERRO ao enviar para usuário %d: %v", userID, err)
		return fmt.Errorf("enviando via FCM: %w", err)
	}

	// Log de sucesso/falha. Em caso de falha, mostra o motivo exato de cada
	// token (ex: token inválido/expirado) e remove tokens mortos do banco.
	if resp.SuccessCount > 0 {
		log.Printf("[fcm] %d notificação(ões) enviada(s) ao usuário %d", resp.SuccessCount, userID)
	}
	if resp.FailureCount > 0 {
		log.Printf("[fcm] %d falha(s) ao notificar usuário %d:", resp.FailureCount, userID)
		for i, r := range resp.Responses {
			if r.Success {
				continue
			}
			log.Printf("[fcm]   - token %d: %v", i, r.Error)
			// Se o token não é mais válido, tira do banco pra não tentar de novo.
			if messaging.IsRegistrationTokenNotRegistered(r.Error) || messaging.IsInvalidArgument(r.Error) {
				if _, delErr := n.db.ExecContext(ctx,
					`DELETE FROM device_tokens WHERE token = ?`, tokens[i],
				); delErr == nil {
					log.Printf("[fcm]     (token inválido removido do banco)")
				}
			}
		}
	}

	return nil
}
