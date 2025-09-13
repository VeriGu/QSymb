if [ -t 0 ]; then
  test -r /root/.opam/opam-init/complete.sh && . /root/.opam/opam-init/complete.sh > /dev/null 2> /dev/null || true

  test -r /root/.opam/opam-init/env_hook.sh && . /root/.opam/opam-init/env_hook.sh > /dev/null 2> /dev/null || true
fi

test -r /root/.opam/opam-init/variables.sh && . /root/.opam/opam-init/variables.sh > /dev/null 2> /dev/null || true
