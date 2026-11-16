package com.usagemonitor

import com.usagemonitor.domain.entity.AppLanguage

/**
 * A ajuda da linha de comando, no idioma do app (decisão D1 da #439, que revogou o
 * "inglês e ASCII" do #418). O motivo daquela regra continua valendo e é tratado na
 * saída: console que não é UTF-8 recebe o texto sem acento (`TerminalConsole`).
 *
 * Lista só o que existe: `--autostart` fica de fora porque é a marca que a chave `Run`
 * deixa para o registro de arranque, e quem a digita à mão só falsifica a origem gravada.
 * Nomes de comandos e opções são sempre em inglês.
 */
internal fun commandLineHelp(language: AppLanguage, version: String = CURRENT_APP_VERSION): String {
    val text = when (language) {
        AppLanguage.PT -> HELP_PT
        AppLanguage.EN -> HELP_EN
        AppLanguage.ZH -> HELP_ZH
    }
    return commandLineVersion(version) + "\n\n" + text
}

private val HELP_PT = """
    |Uso: usage-monitor [comando] [opções]
    |
    |Sem comando, abre o Usage Monitor ou traz o aberto para a frente.
    |
    |Ver o uso
    |  status [--json] [--max-age <tempo>]
    |                     Mostra as cotas de todas as contas e sai.
    |  status --watch [--interval <tempo>]
    |                     A mesma tabela, redesenhada a cada 15 s até o Ctrl+C:
    |                     para deixar aberta num painel do tmux.
    |  check [--max <n>] [--max-age <tempo>] [--quiet]
    |                     Sai com 0 em dia, 1 atenção, 2 crítico, 3 sem leitura.
    |  wait <cota> --below <n> [--timeout <tempo>]
    |                     Espera a cota cair abaixo de n% e sai. O id de cada
    |                     cota sai no status --json.
    |  statusline <claude-code|tmux>
    |                     Uma linha de uso para o Claude Code ou para o tmux.
    |  alerts [--since <tempo>]
    |                     Os avisos de hoje, quando e para onde foram.
    |  analysis [claude|codex|<conta>] [--json]
    |                     Esgotar x reiniciar: quando cada janela reinicia e
    |                     quando a cota acaba no ritmo da última hora e no médio.
    |  tui                O modo interativo: tela cheia, por teclado.
    |
    |Configurar (os mesmos arquivos do app; com ele ou o serviço coletando,
    |quem grava é ele)
    |  setup              Configuração inicial, passo a passo.
    |  config list|get|set
    |                     Os ajustes das Configurações: alertas, teto mensal,
    |                     idioma, atualização, opções do bot e proxy.
    |  sources list|enable|disable
    |                     As fontes monitoradas.
    |  keys set|test|remove <fonte>
    |                     Chaves de API, lidas da entrada, nunca do comando.
    |  accounts list|add|rename|enable|disable|remove|rescan
    |                     Contas do Claude Code e do Codex.
    |  telegram setup|pair|test|status|enable|disable|remove|readonly
    |                     O bot do Telegram.
    |  team join|status|leave
    |                     A integração com o time.
    |  web enable|disable|status|token
    |                     O acesso pela rede local.
    |  update             Confere se há versão nova e diz como atualizar.
    |
    |Servidor sem tela
    |  service install|uninstall|start|stop|restart|status|logs
    |                     O serviço do usuário no systemd (só no Linux).
    |  service run        A coleta em primeiro plano, sem janela.
    |
    |  Os comandos de ver o uso leem a última coleta e não consultam as APIs.
    |  Tempo: 90s, 10m, 2h ou 7d.
    |
    |Opções
    |  -h, --help         Mostra esta ajuda e sai.
    |  -v, --version      Mostra a versão e sai.
    |  --diagnostics      Salva um pacote de diagnóstico na pasta pessoal e sai.
    |                     Só metadados: nenhum prompt, resposta ou credencial.
    |  --locale <pt|en|zh>
    |                     Idioma só desta execução. A escolha gravada não muda.
    |                     Ignorado se o Usage Monitor já estiver aberto.
    |  --user-data-dir <dir>
    |                     Guarda configurações, histórico, chaves e logs desta
    |                     execução em <dir> em vez de ~/.usage-monitor. Outra
    |                     pasta roda ao lado da padrão; nela, a atualização
    |                     automática e o início com o sistema ficam desligados.
""".trimMargin()

private val HELP_EN = """
    |Usage: usage-monitor [command] [options]
    |
    |Without a command, opens Usage Monitor, or brings the running one to the front.
    |
    |See the usage
    |  status [--json] [--max-age <time>]
    |                     Print the quotas of every account and exit.
    |  status --watch [--interval <time>]
    |                     The same table, redrawn every 15 s until Ctrl+C:
    |                     to keep open in a tmux pane.
    |  check [--max <n>] [--max-age <time>] [--quiet]
    |                     Exit with 0 on track, 1 warning, 2 critical, 3 no reading.
    |  wait <quota> --below <n> [--timeout <time>]
    |                     Wait until the quota drops below n% and exit. Each
    |                     quota id is in status --json.
    |  statusline <claude-code|tmux>
    |                     One usage line for Claude Code or for tmux.
    |  alerts [--since <time>]
    |                     Today's alerts, when and where they went.
    |  analysis [claude|codex|<account>] [--json]
    |                     Run out x reset: when each window resets and when the
    |                     quota runs out at the last hour's and the average pace.
    |  tui                The interactive mode: full screen, by keyboard.
    |
    |Configure (the app's own files; with it or the service collecting, it is
    |the one that writes)
    |  setup              First-time setup, step by step.
    |  config list|get|set
    |                     The settings: alerts, monthly budget, language,
    |                     updates, bot options and proxy.
    |  sources list|enable|disable
    |                     The monitored sources.
    |  keys set|test|remove <source>
    |                     API keys, read from the input, never from the command.
    |  accounts list|add|rename|enable|disable|remove|rescan
    |                     Claude Code and Codex accounts.
    |  telegram setup|pair|test|status|enable|disable|remove|readonly
    |                     The Telegram bot.
    |  team join|status|leave
    |                     The team integration.
    |  web enable|disable|status|token
    |                     Access from the local network.
    |  update             Check for a new version and say how to update.
    |
    |Headless server
    |  service install|uninstall|start|stop|restart|status|logs
    |                     The systemd user service (Linux only).
    |  service run        The collection in the foreground, without a window.
    |
    |  The commands that show the usage read the last collection and never call
    |  the APIs. Time: 90s, 10m, 2h or 7d.
    |
    |Options
    |  -h, --help         Print this help and exit.
    |  -v, --version      Print the version and exit.
    |  --diagnostics      Save a diagnostics package to the home folder and exit.
    |                     It holds metadata only: no prompt, response or credential.
    |  --locale <pt|en|zh>
    |                     Interface language for this run only. The saved choice
    |                     does not change. Ignored if Usage Monitor is already open.
    |  --user-data-dir <dir>
    |                     Keep this run's settings, history, keys and logs in <dir>
    |                     instead of ~/.usage-monitor. Another folder runs side by
    |                     side with the default one; there, automatic updates and
    |                     start with the system stay off.
""".trimMargin()

private val HELP_ZH = """
    |用法：usage-monitor [命令] [选项]
    |
    |不带命令时打开 Usage Monitor，或把已打开的窗口带到前台。
    |
    |查看用量
    |  status [--json] [--max-age <时间>]
    |                     显示所有账户的配额后退出。
    |  status --watch [--interval <时间>]
    |                     每 15 秒重绘同一张表，直到按 Ctrl+C。
    |  check [--max <n>] [--max-age <时间>] [--quiet]
    |                     正常退出 0，警告 1，严重 2，无读数 3。
    |  wait <配额> --below <n> [--timeout <时间>]
    |                     等待配额低于 n% 后退出。配额 ID 见 status --json。
    |  statusline <claude-code|tmux>
    |                     为 Claude Code 或 tmux 输出一行用量。
    |  alerts [--since <时间>]
    |                     显示今天的提醒、发送时间和去向。
    |  analysis [claude|codex|<账户>] [--json]
    |                     耗尽分析：窗口何时重置，以及按最近一小时和平均速度
    |                     何时用完。
    |  tui                交互模式：全屏、键盘操作。
    |
    |配置（使用应用自己的文件；应用或服务正在采集时，由它写入）
    |  setup              首次设置，逐步完成。
    |  config list|get|set
    |                     提醒、月度上限、语言、更新、机器人选项和代理。
    |  sources list|enable|disable
    |                     受监视的来源。
    |  keys set|test|remove <来源>
    |                     API 密钥从输入读取，不通过命令参数传递。
    |  accounts list|add|rename|enable|disable|remove|rescan
    |                     Claude Code 和 Codex 账户。
    |  telegram setup|pair|test|status|enable|disable|remove|readonly
    |                     Telegram 机器人。
    |  team join|status|leave
    |                     团队集成。
    |  web enable|disable|status|token
    |                     局域网访问。
    |  update             检查新版本并说明如何更新。
    |
    |无界面服务器
    |  service install|uninstall|start|stop|restart|status|logs
    |                     systemd 用户服务（仅 Linux）。
    |  service run        在前台采集，不打开窗口。
    |
    |  用量命令只读取上次采集，不会调用 API。
    |  时间格式：90s、10m、2h 或 7d。
    |
    |选项
    |  -h, --help         显示本帮助并退出。
    |  -v, --version      显示版本并退出。
    |  --diagnostics      将诊断包保存到用户主目录后退出。
    |                     仅含元数据：不包含提示、回复或凭据。
    |  --locale <pt|en|zh>
    |                     仅本次运行使用的界面语言，不修改已保存的选择。
    |                     如果 Usage Monitor 已打开，此选项会被忽略。
    |  --user-data-dir <dir>
    |                     将本次运行的设置、历史、密钥和日志保存到 <dir>，
    |                     而不是 ~/.usage-monitor。另一个目录可与默认目录并行，
    |                     该目录下自动更新和开机启动保持关闭。
""".trimMargin()
