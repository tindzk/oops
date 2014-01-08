% -----------------------------------------------------------------------
% 
% Class file for PI3, WS 2012/13, Universität Bremen.
%
% $Id: pi3.cls 1794 2012-10-17 13:27:16Z cxl $


\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{guide}[2012/10/05 $Revision: 1794 $  PI3 document class]

\LoadClass[a4paper,11pt,twoside,onecolumn,final]{book}

\RequirePackage[english]{babel}
\RequirePackage[utf8]{inputenc}
\RequirePackage{fancyhdr}
\RequirePackage[a4paper,hmargin={10mm, 20mm},vmargin={15mm}]{geometry}

\bibliographystyle{plain}

\setlength\parskip{1ex}
\setlength\parindent{0ex}
\setlength\headheight{14pt}

\pagestyle{fancy}
\thispagestyle{empty}

\def\thismodule{\jobname}
\renewcommand\include[1]{\def\thismodule{#1}\input{#1.lhs}}

\renewcommand\labelenumi{\alph{enumi})}
\renewcommand\labelenumii{-}