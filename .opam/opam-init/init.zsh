if [[ -o interactive ]]; then
  [[ ! -r /root/.opam/opam-init/complete.zsh ]] || source /root/.opam/opam-init/complete.zsh  > /dev/null 2> /dev/null

  [[ ! -r /root/.opam/opam-init/env_hook.zsh ]] || source /root/.opam/opam-init/env_hook.zsh  > /dev/null 2> /dev/null
fi

[[ ! -r /root/.opam/opam-init/variables.sh ]] || source /root/.opam/opam-init/variables.sh  > /dev/null 2> /dev/null
