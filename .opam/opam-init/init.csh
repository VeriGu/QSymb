if ( $?prompt ) then
  if ( -f /root/.opam/opam-init/env_hook.csh ) source /root/.opam/opam-init/env_hook.csh >& /dev/null
endif

if ( -f /root/.opam/opam-init/variables.csh ) source /root/.opam/opam-init/variables.csh >& /dev/null
