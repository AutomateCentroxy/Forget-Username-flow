package org.gluu.agama.smtp;

import java.util.Map;

class EmailWelcomePt {

    static Map<String, String> get(String userName) {

        String html = """
<table role="presentation" cellspacing="0" cellpadding="0" width="100%" style="background-color:#F2F4F6;">
  <tbody>
    <tr>
      <td align="center">
        <table role="presentation" cellspacing="0" cellpadding="0" width="570" align="center" style="background-color:#FFFFFF;border-radius:4px;margin:20px 0;">
          <tbody>
            <tr>
              <td align="center" style="padding:25px 0;">
                <img src="https://storage.googleapis.com/email_template_staticfiles/Phi_logo320x132_Aug2024.png" width="160" alt="Phi Logo">
              </td>
            </tr>
            <tr>
              <td style="padding:45px;font-family:'Nunito Sans',Helvetica,Arial,sans-serif;font-size:16px;color:#51545E;line-height:1.625;">
                <p>Olá,</p>
                <p>Bem-vindo à <strong>Phi Wallet</strong>! A sua jornada de proteção patrimonial começa agora.</p>

                <p><strong>Detalhes da sua conta:</strong></p>
                <div style="text-align:center;margin:30px 0;">
                  <div style="display:inline-block;background-color:#f5f5f5;color:#AD9269;font-size:28px;font-weight:600;letter-spacing:2px;padding:10px 20px;border-radius:4px;">
                    """ + username + """
                  </div>
                </div>

                <p><strong>Próximo passo: Verificar a sua identidade</strong></p>
                <p>Para garantir que o seu ouro permanece exclusivamente sob o seu controlo, precisamos de verificar a sua identidade. Esta medida de segurança protege o seu ouro e confere-lhe plenos direitos de propriedade.</p>

                <div style="text-align:center;margin:30px 0;">
                  <a href="https://link.phiwallet.com/vll3ylhkeqb" style="background-color:#AD9269;color:#ffffff;padding:14px 28px;text-decoration:none;border-radius:4px;font-weight:600;">
                    Abrir a aplicação
                  </a>
                </div>

                <p>Obrigado por escolher a Phi Wallet.</p>
                <p style="margin-top:30px;">Com os melhores cumprimentos,<br>Equipa Phi Wallet</p>
              </td>
            </tr>
          </tbody>
        </table>

        <table role="presentation" width="570" align="center" style="text-align:center;">
          <tbody>
            <tr>
              <td style="padding:20px;font-size:12px;color:#666;">
                <p style="font-size:14px;font-weight:bold;color:#565555;">Siga-nos em:</p>
                <p>
                  <a href="https://www.facebook.com/PhiWallet"><img src="https://storage.googleapis.com/mwapp_prod_bucket/social_icon_images/facebook.png" style="height:20px;margin:0 5px;"></a>
                  <a href="https://x.com/PhiWallet"><img src="https://storage.googleapis.com/mwapp_prod_bucket/social_icon_images/twitter.png" style="height:20px;margin:0 5px;"></a>
                  <a href="https://www.instagram.com/phi.wallet"><img src="https://storage.googleapis.com/mwapp_prod_bucket/social_icon_images/instagram.png" style="height:20px;margin:0 5px;"></a>
                  <a href="https://www.linkedin.com/company/phiwallet"><img src="https://storage.googleapis.com/mwapp_prod_bucket/social_icon_images/linkedin.png" style="height:20px;margin:0 5px;"></a>
                </p>
                <p style="margin-top:10px;color:#A8AAAF;">
                  Phi Wallet Unipessoal LDA<br>
                  Avenida da Liberdade 262 R/C<br>
                  1250-149 Lisboa, Portugal
                </p>
              </td>
            </tr>
          </tbody>
        </table>

      </td>
    </tr>
  </tbody>
</table>
""";

        return Map.of(
            "subject", "Bem-vindo à Phi Wallet",
            "body", html
        );
    }
}
