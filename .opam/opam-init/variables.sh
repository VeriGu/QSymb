# Prefix of the current opam switch
OPAM_SWITCH_PREFIX='/root/.opam/default'; export OPAM_SWITCH_PREFIX;
# Updated by package ocaml
CAML_LD_LIBRARY_PATH='/usr/local/lib/ocaml/4.13.1/stublibs:/usr/lib/ocaml/stublibs'; export CAML_LD_LIBRARY_PATH;
# Updated by package ocaml
CAML_LD_LIBRARY_PATH='/root/.opam/default/lib/stublibs':"$CAML_LD_LIBRARY_PATH"; export CAML_LD_LIBRARY_PATH;
# Updated by package ocaml
OCAML_TOPLEVEL_PATH='/root/.opam/default/lib/toplevel'; export OCAML_TOPLEVEL_PATH;
# Current opam switch man dir
MANPATH="$MANPATH":'/root/.opam/default/man'; export MANPATH;
# Binary dir for opam switch default
PATH='/root/.opam/default/bin':"$PATH"; export PATH;
